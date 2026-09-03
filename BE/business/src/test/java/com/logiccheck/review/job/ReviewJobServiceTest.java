package com.logiccheck.review.job;

import com.logiccheck.document.port.DocumentQueryPort;
import com.logiccheck.document.port.DocumentQueryPort.DocumentMetaView;
import com.logiccheck.global.exception.ErrorCode;
import com.logiccheck.review.job.dto.JobSummaryView;
import com.logiccheck.review.support.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 명세 16 · 17 · 18 의 선행 조건과 상태 전이 (DEV3 D-1 · D-2). */
class ReviewJobServiceTest {

    private static final JobSummaryView SUMMARY =
            new JobSummaryView(8, java.util.Map.of("ERROR", 3L, "WARNING", 3L, "INFO", 2L), 2, 1, 1, 6);

    private ReviewJobRepository repository;
    private String parseStatus;
    private boolean owned;
    private ReviewJobService service;
    private com.logiccheck.review.pipeline.ReviewPipeline pipeline;

    @BeforeEach
    void setUp() {
        repository = mock(ReviewJobRepository.class);
        parseStatus = "DONE";
        owned = true;

        DocumentQueryPort port = (documentId, userId) -> owned
                ? Optional.of(new DocumentMetaView(documentId, userId, "스텁 문서", 21, parseStatus))
                : Optional.empty();
        pipeline = mock(com.logiccheck.review.pipeline.ReviewPipeline.class);
        service = new ReviewJobService(repository, port, jobId -> SUMMARY, pipeline);

        when(repository.existsByDocumentIdAndStatusIn(anyLong(), any())).thenReturn(false);
        when(repository.saveAndFlush(any(ReviewJob.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), 42L));
    }

    @Test
    void 분석을_시작하면_PENDING_IN_REVIEW_Job이_생성된다() {
        ReviewJob job = service.start(1L, 7L);

        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(job.getReviewStatus()).isEqualTo(ReviewStatus.IN_REVIEW);
        assertThat(job.getDocumentId()).isEqualTo(1L);
    }

    @Test
    void 분석을_시작하면_파이프라인이_트리거된다() {
        ReviewJob job = service.start(1L, 7L);

        org.mockito.Mockito.verify(pipeline).runAsync(job.getId(), 7L);
    }

    @Test
    void 선행_조건_위반이면_파이프라인을_트리거하지_않는다() {
        parseStatus = "PARSING";

        assertThatErrorCode(() -> service.start(1L, 7L), ErrorCode.DOCUMENT_NOT_READY);
        org.mockito.Mockito.verify(pipeline, org.mockito.Mockito.never()).runAsync(anyLong(), anyLong());
    }

    @Test
    void 생성_직후_startedAt_finishedAt_rulesetVersion_은_모두_null_이다() {
        ReviewJob job = service.start(1L, 7L);

        assertThat(job.getStartedAt()).isNull();
        assertThat(job.getFinishedAt()).isNull();
        assertThat(job.getRulesetVersion()).isNull();
        assertThat(job.getCompletedAt()).isNull();
    }

    @Test
    void 파싱이_끝나지_않은_문서는_DOCUMENT_NOT_READY_다() {
        parseStatus = "PARSING";

        assertThatErrorCode(() -> service.start(1L, 7L), ErrorCode.DOCUMENT_NOT_READY);
    }

    @Test
    void 진행중인_Job이_있으면_JOB_ALREADY_RUNNING_이다() {
        when(repository.existsByDocumentIdAndStatusIn(eq(1L), any())).thenReturn(true);

        assertThatErrorCode(() -> service.start(1L, 7L), ErrorCode.JOB_ALREADY_RUNNING);
    }

    @Test
    void 부분_유니크_인덱스_위반은_JOB_ALREADY_RUNNING_으로_변환된다() {
        when(repository.saveAndFlush(any(ReviewJob.class)))
                .thenThrow(new DataIntegrityViolationException("ux_review_jobs_active"));

        assertThatErrorCode(() -> service.start(1L, 7L), ErrorCode.JOB_ALREADY_RUNNING);
    }

    @Test
    void 소유자가_아니면_FORBIDDEN_이다() {
        owned = false;

        assertThatErrorCode(() -> service.start(1L, 7L), ErrorCode.FORBIDDEN);
    }

    @Test
    void 없는_Job_조회는_NOT_FOUND_다() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatErrorCode(() -> service.findForOwner(999L, 7L), ErrorCode.NOT_FOUND);
    }

    @Test
    void 타인의_Job_조회는_FORBIDDEN_이다() {
        when(repository.findById(42L)).thenReturn(Optional.of(withId(ReviewJob.pending(1L), 42L)));
        owned = false;

        assertThatErrorCode(() -> service.findForOwner(42L, 7L), ErrorCode.FORBIDDEN);
    }

    @Test
    void 분석하지_않은_문서의_최근_Job_조회는_NO_REVIEW_JOB_이다() {
        when(repository.findFirstByDocumentIdOrderByIdDesc(1L)).thenReturn(Optional.empty());

        assertThatErrorCode(() -> service.findLatestForOwner(1L, 7L), ErrorCode.NO_REVIEW_JOB);
    }

    @Test
    void 조회_응답의_문서_정보는_DocumentQueryPort_에서_온다() {
        when(repository.findById(42L)).thenReturn(Optional.of(withId(ReviewJob.pending(1L), 42L)));

        DocumentMetaView meta = service.findForOwner(42L, 7L).document();

        assertThat(meta.title()).isEqualTo("스텁 문서");
        assertThat(meta.pageCount()).isEqualTo(21);
    }

    @Test
    void summary_는_status_가_DONE_일_때만_채워진다() {
        ReviewJob done = withStatus(withId(ReviewJob.pending(1L), 42L), JobStatus.DONE);
        when(repository.findById(42L)).thenReturn(Optional.of(done));

        assertThat(service.findForOwner(42L, 7L).summary()).isEqualTo(SUMMARY);
    }

    @Test
    void summary_는_DONE_이_아니면_null_이다() {
        for (JobStatus status : new JobStatus[]{JobStatus.PENDING, JobStatus.RUNNING, JobStatus.FAILED}) {
            ReviewJob job = withStatus(withId(ReviewJob.pending(1L), 42L), status);
            when(repository.findById(42L)).thenReturn(Optional.of(job));

            assertThat(service.findForOwner(42L, 7L).summary())
                    .as("status = %s", status)
                    .isNull();
        }
    }

    @Test
    void 검토_완료는_review_status_와_completed_at_만_바꾼다() {
        ReviewJob done = withStatus(withId(ReviewJob.pending(1L), 42L), JobStatus.DONE);
        when(repository.findById(42L)).thenReturn(Optional.of(done));

        service.completeReview(42L, 7L);

        assertThat(done.getReviewStatus()).isEqualTo(ReviewStatus.COMPLETED);
        assertThat(done.getCompletedAt()).isNotNull();
        assertThat(done.getStatus()).as("status 는 건드리지 않는다").isEqualTo(JobStatus.DONE);
    }

    @Test
    void 재완료_요청은_JOB_ALREADY_COMPLETED_다() {
        ReviewJob done = withStatus(withId(ReviewJob.pending(1L), 42L), JobStatus.DONE);
        done.completeReview();
        when(repository.findById(42L)).thenReturn(Optional.of(done));

        assertThatErrorCode(() -> service.completeReview(42L, 7L), ErrorCode.JOB_ALREADY_COMPLETED);
    }

    @Test
    void 분석이_끝나지_않은_Job_의_완료_요청은_DOCUMENT_NOT_READY_다() {
        for (JobStatus status : new JobStatus[]{JobStatus.PENDING, JobStatus.RUNNING, JobStatus.FAILED}) {
            ReviewJob job = withStatus(withId(ReviewJob.pending(1L), 42L), status);
            when(repository.findById(42L)).thenReturn(Optional.of(job));

            assertThatThrownBy(() -> service.completeReview(42L, 7L))
                    .as("status = %s", status)
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.DOCUMENT_NOT_READY);
        }
    }

    @Test
    void 완료된_Job_도_terminal_과_status_는_그대로다() {
        ReviewJob done = withStatus(withId(ReviewJob.pending(1L), 42L), JobStatus.DONE);
        done.completeReview();

        assertThat(done.getStatus().isTerminal()).isTrue();
        assertThat(done.getStatus()).isEqualTo(JobStatus.DONE);
        assertThat(done.isReviewCompleted()).isTrue();
    }

    @Test
    void terminal_은_DONE_FAILED_에만_true_다() {
        assertThat(JobStatus.PENDING.isTerminal()).isFalse();
        assertThat(JobStatus.RUNNING.isTerminal()).isFalse();
        assertThat(JobStatus.DONE.isTerminal()).isTrue();
        assertThat(JobStatus.FAILED.isTerminal()).isTrue();
    }

    @Test
    void ACTIVE_집합은_DB_부분_유니크_인덱스와_같다() {
        assertThat(JobStatus.ACTIVE).containsExactlyInAnyOrder(JobStatus.PENDING, JobStatus.RUNNING);
    }

    private static ReviewJob withId(ReviewJob job, long id) {
        ReflectionTestUtils.setField(job, "id", id);
        return job;
    }

    private static ReviewJob withStatus(ReviewJob job, JobStatus status) {
        ReflectionTestUtils.setField(job, "status", status);
        return job;
    }

    private static void assertThatErrorCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
