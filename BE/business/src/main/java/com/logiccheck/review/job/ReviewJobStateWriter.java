package com.logiccheck.review.job;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.logiccheck.ai.ReviewAiResult;
import com.logiccheck.ai.ReviewAiResult.AiEvidence;
import com.logiccheck.ai.ReviewAiResult.AiFinding;
import com.logiccheck.review.finding.Finding;
import com.logiccheck.review.finding.FindingMethod;
import com.logiccheck.review.finding.FindingRepository;
import com.logiccheck.review.finding.FindingType;

/**
 * 백그라운드 진행 상황을 단계마다 따로 커밋한다.
 *
 * <p>한 트랜잭션으로 묶으면 작업이 끝날 때까지 아무것도 보이지 않아서, 진행률을
 * 물어보는 요청에 계속 0%만 돌려주게 된다. 그래서 상태 변경을 잘게 나눠 커밋한다.
 */
@Component
public class ReviewJobStateWriter {

    private final ReviewJobRepository reviewJobRepository;
    private final FindingRepository findingRepository;

    public ReviewJobStateWriter(ReviewJobRepository reviewJobRepository, FindingRepository findingRepository) {
        this.reviewJobRepository = reviewJobRepository;
        this.findingRepository = findingRepository;
    }

    @Transactional
    public void beginParsing(Long jobId) {
        reviewJobRepository.findById(jobId).ifPresent(job -> job.beginParsing(ReviewJobSteps.running(0)));
    }

    @Transactional
    public void advance(Long jobId, List<String> steps) {
        reviewJobRepository.findById(jobId).ifPresent(job -> job.advance(steps));
    }

    @Transactional
    public void beginAnalyzing(Long jobId, List<PartialFailure> partialFailures) {
        reviewJobRepository.findById(jobId)
                .ifPresent(job -> job.beginAnalyzing(ReviewJobSteps.allDone(), partialFailures));
    }

    @Transactional
    public void succeed(Long jobId) {
        reviewJobRepository.findById(jobId).ifPresent(ReviewJob::succeed);
    }

    @Transactional
    public void fail(Long jobId, String errorCode) {
        reviewJobRepository.findById(jobId).ifPresent(job -> job.fail(errorCode));
    }

    /** 한 건씩 커밋해야 분석 중에도 발견된 항목이 화면에 차례로 나타난다. */
    @Transactional
    public void saveFinding(Long jobId, AiFinding source, int orderNo) {
        Finding finding = Finding.of(jobId,
                FindingType.valueOf(source.type()),
                FindingMethod.valueOf(source.method()),
                source.sectionId(),
                source.page(),
                source.title(),
                source.description(),
                BigDecimal.valueOf(source.confidence()),
                expression(source),
                number(source, ReviewAiResult.AiCalculation::expected),
                number(source, ReviewAiResult.AiCalculation::actual),
                orderNo);
        for (AiEvidence evidence : source.evidence()) {
            finding.addEvidence(evidence.anchorId(), evidence.page(), evidence.label(), null);
        }
        findingRepository.save(finding);
    }

    private String expression(AiFinding source) {
        return source.calculation() == null ? null : source.calculation().expression();
    }

    private BigDecimal number(AiFinding source,
                               java.util.function.Function<ReviewAiResult.AiCalculation, Double> field) {
        if (source.calculation() == null) {
            return null;
        }
        Double value = field.apply(source.calculation());
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
