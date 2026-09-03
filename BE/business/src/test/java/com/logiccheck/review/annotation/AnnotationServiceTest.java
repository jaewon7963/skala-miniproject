package com.logiccheck.review.annotation;

import com.logiccheck.document.port.DocumentQueryPort.DocumentMetaView;
import com.logiccheck.global.exception.ErrorCode;
import com.logiccheck.review.annotation.Annotation.Anchor;
import com.logiccheck.review.finding.Finding;
import com.logiccheck.review.finding.FindingRepository;
import com.logiccheck.review.job.JobStatus;
import com.logiccheck.review.job.ReviewJob;
import com.logiccheck.review.job.ReviewJobService;
import com.logiccheck.review.job.ReviewJobService.JobWithDocument;
import com.logiccheck.review.support.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 명세 24 · 25 · 26 · 27 (DEV3 D-7). */
class AnnotationServiceTest {

    private static final DocumentMetaView META = new DocumentMetaView(1L, 7L, "스텁 문서", 21, "DONE");

    private AnnotationRepository annotationRepository;
    private FindingRepository findingRepository;
    private ReviewJobService reviewJobService;
    private AnnotationService service;
    private ReviewJob job;

    @BeforeEach
    void setUp() {
        annotationRepository = mock(AnnotationRepository.class);
        findingRepository = mock(FindingRepository.class);
        reviewJobService = mock(ReviewJobService.class);
        service = new AnnotationService(annotationRepository, findingRepository, reviewJobService);

        job = ReviewJob.pending(1L);
        ReflectionTestUtils.setField(job, "id", 42L);
        ReflectionTestUtils.setField(job, "status", JobStatus.DONE);
        when(reviewJobService.findForOwner(42L, 7L)).thenReturn(new JobWithDocument(job, META, null));
        when(annotationRepository.save(any(Annotation.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void authorId_는_요청이_아니라_JWT_사용자로_고정된다() {
        Annotation created = service.create(42L, 7L, null, "재무팀 확인 필요", null);

        assertThat(created.getAuthorId()).isEqualTo(7L);
    }

    @Test
    void findingId_가_없으면_PDF_자유_주석이다() {
        Annotation created = service.create(42L, 7L, null, "메모", null);

        assertThat(created.getFinding()).isNull();
    }

    @Test
    void findingId_가_있으면_Finding_주석이고_좌표가_없으면_첫_evidence_를_복사한다() {
        Finding finding = findingWithEvidence(5L);
        when(findingRepository.findWithEvidenceById(5L)).thenReturn(Optional.of(finding));

        Annotation created = service.create(42L, 7L, "5", "표 재계산 결과 첨부 요청", null);

        assertThat(created.getFinding()).isSameAs(finding);
        assertThat(created.getPageNo()).isEqualTo(11);
        assertThat(created.getQuote()).isEqualTo("2027년 매출 24억 원");
        assertThat(created.getBboxX()).isEqualByComparingTo("0.12");
    }

    @Test
    void 다른_Job_의_Finding_을_지정하면_NOT_FOUND_다() {
        ReviewJob other = ReviewJob.pending(2L);
        ReflectionTestUtils.setField(other, "id", 99L);
        Finding finding = findingWithEvidence(5L);
        ReflectionTestUtils.setField(finding, "job", other);
        when(findingRepository.findWithEvidenceById(5L)).thenReturn(Optional.of(finding));

        assertThatErrorCode(() -> service.create(42L, 7L, "5", "메모", null), ErrorCode.NOT_FOUND);
    }

    @Test
    void 없는_Finding_을_지정하면_NOT_FOUND_다() {
        when(findingRepository.findWithEvidenceById(5L)).thenReturn(Optional.empty());

        assertThatErrorCode(() -> service.create(42L, 7L, "5", "메모", null), ErrorCode.NOT_FOUND);
    }

    @Test
    void 좌표를_직접_보내면_그대로_저장된다() {
        Anchor anchor = new Anchor(3, "인용문",
                new BigDecimal("0.10"), new BigDecimal("0.20"),
                new BigDecimal("0.30"), new BigDecimal("0.40"));

        Annotation created = service.create(42L, 7L, null, "메모", anchor);

        assertThat(created.getPageNo()).isEqualTo(3);
        assertThat(created.hasBbox()).isTrue();
        assertThat(created.getBboxH()).isEqualByComparingTo("0.40");
    }

    @Test
    void 목록은_soft_delete_된_행을_제외한다() {
        when(annotationRepository.findByJobIdAndDeletedAtIsNullOrderByIdAsc(42L))
                .thenReturn(List.of(annotation(1L)));

        assertThat(service.findAllOfJob(42L, 7L)).hasSize(1);
    }

    @Test
    void 삭제는_deleted_at_을_채운다() {
        Annotation target = annotation(1L);
        when(annotationRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(target));

        service.delete(1L, 7L);

        assertThat(target.getDeletedAt()).isNotNull();
    }

    @Test
    void 이미_삭제된_주석은_NOT_FOUND_다() {
        when(annotationRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatErrorCode(() -> service.delete(1L, 7L), ErrorCode.NOT_FOUND);
    }

    @Test
    void 수정은_본문만_바꾼다() {
        Annotation target = annotation(1L);
        when(annotationRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(target));

        service.updateBody(1L, 7L, "수정된 메모");

        assertThat(target.getBody()).isEqualTo("수정된 메모");
        assertThat(target.getAuthorId()).isEqualTo(7L);
    }

    @Test
    void 타인_Job_의_주석은_소유권_검사에서_막힌다() {
        Annotation target = annotation(1L);
        when(annotationRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(target));
        when(reviewJobService.findForOwner(anyLong(), anyLong()))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        assertThatErrorCode(() -> service.updateBody(1L, 9L, "메모"), ErrorCode.FORBIDDEN);
    }

    private Annotation annotation(long id) {
        Annotation annotation = Annotation.create(job, null, 7L, "원래 메모", null);
        ReflectionTestUtils.setField(annotation, "id", id);
        return annotation;
    }

    private Finding findingWithEvidence(long id) {
        Finding finding = new Finding() {
        };
        ReflectionTestUtils.setField(finding, "id", id);
        ReflectionTestUtils.setField(finding, "job", job);
        com.logiccheck.review.finding.FindingEvidence evidence =
                new com.logiccheck.review.finding.FindingEvidence() {
                };
        ReflectionTestUtils.setField(evidence, "id", id * 10);
        ReflectionTestUtils.setField(evidence, "pageNo", 11);
        ReflectionTestUtils.setField(evidence, "quote", "2027년 매출 24억 원");
        ReflectionTestUtils.setField(evidence, "bboxX", new BigDecimal("0.120000"));
        ReflectionTestUtils.setField(evidence, "bboxY", new BigDecimal("0.310000"));
        ReflectionTestUtils.setField(evidence, "bboxW", new BigDecimal("0.660000"));
        ReflectionTestUtils.setField(evidence, "bboxH", new BigDecimal("0.030000"));
        ReflectionTestUtils.setField(finding, "evidence", List.of(evidence));
        return finding;
    }

    private static void assertThatErrorCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
