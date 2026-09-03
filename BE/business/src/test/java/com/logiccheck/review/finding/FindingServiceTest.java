package com.logiccheck.review.finding;

import com.logiccheck.document.port.DocumentQueryPort.DocumentMetaView;
import com.logiccheck.global.exception.ErrorCode;
import com.logiccheck.review.job.JobStatus;
import com.logiccheck.review.job.ReviewJob;
import com.logiccheck.review.job.ReviewJobService;
import com.logiccheck.review.job.ReviewJobService.JobWithDocument;
import com.logiccheck.review.support.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 명세 21 · 22 (DEV3 D-4 · D-5). */
class FindingServiceTest {

    private static final DocumentMetaView META =
            new DocumentMetaView(1L, 7L, "스텁 문서", 21, "DONE");

    private FindingRepository findingRepository;
    private ReviewJobService reviewJobService;
    private FindingService service;

    @BeforeEach
    void setUp() {
        findingRepository = mock(FindingRepository.class);
        reviewJobService = mock(ReviewJobService.class);
        service = new FindingService(findingRepository, reviewJobService);
    }

    @Test
    void 목록은_severity_의미순서_다음_confidence_내림차순으로_정렬된다() {
        ReviewJob job = givenJob(JobStatus.DONE);
        when(findingRepository.findByJobId(42L)).thenReturn(List.of(
                FindingFixtures.finding(1, job, Severity.INFO, "0.99", null),
                FindingFixtures.finding(2, job, Severity.WARNING, "0.50", null),
                FindingFixtures.finding(3, job, Severity.ERROR, "0.70", null),
                FindingFixtures.finding(4, job, Severity.WARNING, "0.80", null),
                FindingFixtures.finding(5, job, Severity.ERROR, "0.90", null)
        ));

        assertThat(service.findAllOfJob(42L, 7L))
                .extracting(Finding::getSeverity, f -> f.getConfidence().toPlainString())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(Severity.ERROR, "0.90"),
                        org.assertj.core.groups.Tuple.tuple(Severity.ERROR, "0.70"),
                        org.assertj.core.groups.Tuple.tuple(Severity.WARNING, "0.80"),
                        org.assertj.core.groups.Tuple.tuple(Severity.WARNING, "0.50"),
                        org.assertj.core.groups.Tuple.tuple(Severity.INFO, "0.99"));
    }

    @Test
    void confidence_가_null_이면_같은_severity_안에서_뒤로_간다() {
        ReviewJob job = givenJob(JobStatus.DONE);
        when(findingRepository.findByJobId(42L)).thenReturn(List.of(
                FindingFixtures.finding(1, job, Severity.ERROR, null, null),
                FindingFixtures.finding(2, job, Severity.ERROR, "0.10", null)
        ));

        assertThat(service.findAllOfJob(42L, 7L)).extracting(Finding::getId).containsExactly(2L, 1L);
    }

    @Test
    void 분석_미완료_Job_은_빈_배열이다() {
        for (JobStatus status : new JobStatus[]{JobStatus.PENDING, JobStatus.RUNNING}) {
            givenJob(status);

            assertThat(service.findAllOfJob(42L, 7L)).as("status = %s", status).isEmpty();
        }
    }

    @Test
    void FAILED_는_종료_상태이므로_저장된_항목을_그대로_반환한다() {
        ReviewJob job = givenJob(JobStatus.FAILED);
        when(findingRepository.findByJobId(42L))
                .thenReturn(List.of(FindingFixtures.finding(1, job, Severity.ERROR, "0.90", null)));

        assertThat(service.findAllOfJob(42L, 7L)).hasSize(1);
    }

    @Test
    void 없는_Finding_상세는_NOT_FOUND_다() {
        when(findingRepository.findWithEvidenceById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findOne(999L, 7L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void 타인_Job_의_Finding_상세는_소유권_검사에서_막힌다() {
        ReviewJob job = FindingFixtures.job(42L, JobStatus.DONE);
        when(findingRepository.findWithEvidenceById(1L))
                .thenReturn(Optional.of(FindingFixtures.finding(1, job, Severity.ERROR, "0.9", null)));
        when(reviewJobService.findForOwner(anyLong(), anyLong()))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        assertThatThrownBy(() -> service.findOne(1L, 7L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void method_는_rule_id_유무로_파생된다() {
        ReviewJob job = FindingFixtures.job(42L, JobStatus.DONE);

        assertThat(FindingFixtures.finding(1, job, Severity.ERROR, "0.9", 7L).method())
                .isEqualTo(FindingMethod.DETERMINISTIC);
        assertThat(FindingFixtures.finding(2, job, Severity.ERROR, "0.9", null).method())
                .isEqualTo(FindingMethod.RAG);
    }

    private ReviewJob givenJob(JobStatus status) {
        ReviewJob job = FindingFixtures.job(42L, status);
        when(reviewJobService.findForOwner(42L, 7L)).thenReturn(new JobWithDocument(job, META, null));
        return job;
    }
}
