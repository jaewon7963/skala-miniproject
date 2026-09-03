package com.logiccheck.review.finding;

import com.logiccheck.document.port.DocumentQueryPort.DocumentMetaView;
import com.logiccheck.global.exception.ErrorCode;
import com.logiccheck.review.job.JobStatus;
import com.logiccheck.review.job.ReviewJob;
import com.logiccheck.review.job.ReviewJobService;
import com.logiccheck.review.annotation.Annotation;
import com.logiccheck.review.annotation.AnnotationService;
import com.logiccheck.review.job.ReviewJobService.JobWithDocument;
import com.logiccheck.review.support.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 명세 21 · 22 (DEV3 D-4 · D-5). */
class FindingServiceTest {

    private static final DocumentMetaView META =
            new DocumentMetaView(1L, 7L, "스텁 문서", 21, "DONE");

    private FindingRepository findingRepository;
    private FindingDecisionRepository findingDecisionRepository;
    private ReviewJobService reviewJobService;
    private AnnotationService annotationService;
    private FindingService service;

    @BeforeEach
    void setUp() {
        findingRepository = mock(FindingRepository.class);
        findingDecisionRepository = mock(FindingDecisionRepository.class);
        reviewJobService = mock(ReviewJobService.class);
        annotationService = mock(AnnotationService.class);
        service = new FindingService(findingRepository, findingDecisionRepository,
                reviewJobService, annotationService);
        when(findingDecisionRepository.save(any(FindingDecision.class)))
                .thenAnswer(i -> i.getArgument(0));
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

    @Test
    void 판정하면_status_가_바뀌고_decided_at_이_기록된다() {
        Finding finding = givenOwnedFinding();

        Finding decided = service.decide(1L, 7L, DecisionAction.ACCEPT, null, null);

        assertThat(decided.getStatus()).isEqualTo(FindingStatus.ACCEPTED);
        assertThat(decided.getDecidedAt()).isNotNull();
        assertThat(finding).isSameAs(decided);
    }

    @Test
    void 판정하면_finding_decisions_에_이력이_쌓인다() {
        givenOwnedFinding();

        service.decide(1L, 7L, DecisionAction.REJECT, "재무팀 확인 결과 오류 아님", null);

        org.mockito.ArgumentCaptor<FindingDecision> captor =
                org.mockito.ArgumentCaptor.forClass(FindingDecision.class);
        org.mockito.Mockito.verify(findingDecisionRepository).save(captor.capture());
        FindingDecision saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo(DecisionAction.REJECT);
        assertThat(saved.getBeforeStatus()).isEqualTo(FindingStatus.OPEN);
        assertThat(saved.getAfterStatus()).isEqualTo(FindingStatus.REJECTED);
        assertThat(saved.getActorId()).as("actorId 는 JWT 사용자").isEqualTo(7L);
        assertThat(saved.getNote()).isEqualTo("재무팀 확인 결과 오류 아님");
    }

    @Test
    void 재판정하면_이력이_두_건_쌓인다() {
        givenOwnedFinding();

        service.decide(1L, 7L, DecisionAction.ACCEPT, null, null);
        Finding decided = service.decide(1L, 7L, DecisionAction.REJECT, null, null);

        assertThat(decided.getStatus()).isEqualTo(FindingStatus.REJECTED);
        org.mockito.Mockito.verify(findingDecisionRepository, org.mockito.Mockito.times(2))
                .save(any(FindingDecision.class));
    }

    @Test
    void 검토가_완료된_Job_의_Finding_판정은_JOB_ALREADY_COMPLETED_다() {
        ReviewJob job = FindingFixtures.job(42L, JobStatus.DONE);
        job.completeReview();
        Finding finding = FindingFixtures.finding(1, job, Severity.ERROR, "0.9", null);
        when(findingRepository.findWithEvidenceById(1L)).thenReturn(Optional.of(finding));
        when(reviewJobService.findForOwner(42L, 7L)).thenReturn(new JobWithDocument(job, META, null));

        assertThatThrownBy(() -> service.decide(1L, 7L, DecisionAction.ACCEPT, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.JOB_ALREADY_COMPLETED);
        assertThat(finding.getStatus()).as("판정이 적용되지 않아야 한다").isEqualTo(FindingStatus.OPEN);
    }

    @Test
    void annotationBody_가_있으면_같은_호출에서_주석을_만든다() {
        Finding finding = givenOwnedFinding();

        service.decide(1L, 7L, DecisionAction.ACCEPT, null, "표 5-1 재계산 결과 첨부 요청");

        org.mockito.Mockito.verify(annotationService)
                .createForFinding(finding, 7L, "표 5-1 재계산 결과 첨부 요청");
    }

    @Test
    void annotationBody_가_비어_있으면_주석을_만들지_않는다() {
        givenOwnedFinding();

        service.decide(1L, 7L, DecisionAction.ACCEPT, null, "   ");

        org.mockito.Mockito.verify(annotationService, org.mockito.Mockito.never())
                .createForFinding(any(), anyLong(), anyString());
    }

    @Test
    void 주석_저장이_실패하면_예외가_전파되어_판정도_롤백된다() {
        givenOwnedFinding();
        when(annotationService.createForFinding(any(), anyLong(), anyString()))
                .thenThrow(new IllegalStateException("주석 저장 실패"));

        assertThatThrownBy(() -> service.decide(1L, 7L, DecisionAction.ACCEPT, null, "메모"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 없는_Finding_판정은_NOT_FOUND_다() {
        when(findingRepository.findWithEvidenceById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.decide(999L, 7L, DecisionAction.ACCEPT, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void DecisionAction_은_결과_상태로_사상된다() {
        assertThat(DecisionAction.ACCEPT.resultStatus()).isEqualTo(FindingStatus.ACCEPTED);
        assertThat(DecisionAction.REJECT.resultStatus()).isEqualTo(FindingStatus.REJECTED);
    }

    private Finding givenOwnedFinding() {
        ReviewJob job = FindingFixtures.job(42L, JobStatus.DONE);
        Finding finding = FindingFixtures.finding(1, job, Severity.ERROR, "0.9", null);
        when(findingRepository.findWithEvidenceById(1L)).thenReturn(Optional.of(finding));
        when(reviewJobService.findForOwner(42L, 7L)).thenReturn(new JobWithDocument(job, META, null));
        return finding;
    }

    private ReviewJob givenJob(JobStatus status) {
        ReviewJob job = FindingFixtures.job(42L, status);
        when(reviewJobService.findForOwner(42L, 7L)).thenReturn(new JobWithDocument(job, META, null));
        return job;
    }
}
