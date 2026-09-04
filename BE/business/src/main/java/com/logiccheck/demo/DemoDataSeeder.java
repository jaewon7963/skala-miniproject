package com.logiccheck.demo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.logiccheck.document.entity.ParseStatus;
import com.logiccheck.document.repository.DocumentRepository;
import com.logiccheck.document.service.DocumentCommandService;
import com.logiccheck.review.finding.FindingRepository;
import com.logiccheck.review.job.JobStatus;
import com.logiccheck.review.job.ReviewJob;
import com.logiccheck.review.job.ReviewJobRepository;
import com.logiccheck.review.job.ReviewJobService;
import com.logiccheck.user.entity.User;
import com.logiccheck.user.repository.UserRepository;

/**
 * 시연·평가용 기본 계정과 문서를 기동 시 한 번 심는다. {@code demo.seed.enabled=true} 일 때만 돈다.
 *
 * <p>행을 직접 INSERT 하지 않고 <b>실제 업로드-파싱-분석 경로를 그대로 태운다.</b>
 * {@code pages.blocks} 의 블록 id와 검토 근거의 {@code anchorId} 가 한 글자만 어긋나도 화면의
 * 하이라이트 이동이 조용히 죽는데, 손으로 쓴 시드 SQL은 파서가 바뀌는 순간 그렇게 된다.
 * 분석기는 외부 모델이 아니라 규칙 기반이라 이 경로에 네트워크 의존이 없고 결과도 매번 같다.
 *
 * <p>계정과 문서는 각각 따로 판단한다. 볼륨만 비우고 DB는 남은 상태에서도 문서가 다시 심긴다.
 */
@Component
@ConditionalOnProperty(name = "demo.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final String DEMO_PDF = "demo/bizxray-demo-plan.pdf";
    private static final String DEMO_FILENAME = "AI 매장 안내 로봇 사업계획서.pdf";
    private static final long POLL_INTERVAL_MS = 300;
    private static final long PARSE_TIMEOUT_MS = 60_000;
    private static final long REVIEW_TIMEOUT_MS = 120_000;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DocumentCommandService documentCommandService;
    private final DocumentRepository documentRepository;
    private final ReviewJobService reviewJobService;
    private final ReviewJobRepository reviewJobRepository;
    private final FindingRepository findingRepository;
    private final String email;
    private final String password;

    public DemoDataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           DocumentCommandService documentCommandService, DocumentRepository documentRepository,
                           ReviewJobService reviewJobService, ReviewJobRepository reviewJobRepository,
                           FindingRepository findingRepository,
                           @Value("${demo.seed.email}") String email,
                           @Value("${demo.seed.password}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.documentCommandService = documentCommandService;
        this.documentRepository = documentRepository;
        this.reviewJobService = reviewJobService;
        this.reviewJobRepository = reviewJobRepository;
        this.findingRepository = findingRepository;
        this.email = email;
        this.password = password;
    }

    /** 시드가 실패해도 서버는 떠야 한다. 시연 데이터가 없을 뿐, 회원가입부터 하면 그만이다. */
    @Override
    public void run(ApplicationArguments args) {
        try {
            Long ownerId = seedAccount();
            seedDocument(ownerId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("시연 데이터 준비가 중단되었습니다");
        } catch (Exception e) {
            log.warn("시연 데이터를 준비하지 못했습니다. 서버는 그대로 뜹니다.", e);
        }
    }

    private Long seedAccount() {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            log.info("시연 계정이 이미 있습니다: {}", email);
            return existing.get().getId();
        }
        User user = userRepository.saveAndFlush(User.create(email, passwordEncoder.encode(password)));
        log.info("시연 계정을 만들었습니다: {} / {}", email, password);
        return user.getId();
    }

    private void seedDocument(Long ownerId) throws IOException, InterruptedException {
        List<DocumentRepository.DocIdStatus> owned = documentRepository.findByOwnerIdAndDeletedAtIsNull(ownerId);
        if (!owned.isEmpty()) {
            log.info("시연 계정에 문서가 이미 {}건 있어 건너뜁니다", owned.size());
            return;
        }

        Long documentId = Long.valueOf(documentCommandService.upload(ownerId, readDemoPdf(), DEMO_FILENAME).id());
        log.info("시연 문서를 업로드했습니다: {} (id={})", DEMO_FILENAME, documentId);

        awaitParsed(documentId);
        Long jobId = Long.valueOf(reviewJobService.create(ownerId, documentId).id());
        ReviewJob job = awaitReviewed(jobId);

        if (job.getStatus() == JobStatus.DONE) {
            log.info("시연 문서 분석 완료 — 검토 항목 {}건, 검토 대기 상태로 둡니다", findingRepository.countByJobId(jobId));
        } else {
            log.warn("시연 문서 분석이 {} 상태로 끝났습니다", job.getStatus());
        }
    }

    private byte[] readDemoPdf() throws IOException {
        try (InputStream in = new ClassPathResource(DEMO_PDF).getInputStream()) {
            return in.readAllBytes();
        }
    }

    /** 업로드 직후 파싱은 커밋 뒤 비동기로 돈다. 분석을 걸기 전에 끝나기를 기다린다. */
    private void awaitParsed(Long documentId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + PARSE_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            ParseStatus status = documentRepository.findById(documentId).orElseThrow().getParseStatus();
            if (status == ParseStatus.DONE) {
                return;
            }
            if (status == ParseStatus.FAILED) {
                throw new IllegalStateException("시연 문서 파싱이 실패했습니다");
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        throw new IllegalStateException("시연 문서 파싱이 제한 시간 안에 끝나지 않았습니다");
    }

    private ReviewJob awaitReviewed(Long jobId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + REVIEW_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            ReviewJob job = reviewJobRepository.findById(jobId).orElseThrow();
            if (job.getStatus().isTerminal()) {
                return job;
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        throw new IllegalStateException("시연 문서 분석이 제한 시간 안에 끝나지 않았습니다");
    }
}
