package com.logiccheck.review.finding;

import com.logiccheck.review.job.dto.JobSummaryView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 명세 17 의 summary 집계 (DEV3 D-3). */
class FindingSummaryProviderTest {

    private final FindingRepository repository = mock(FindingRepository.class);
    private final FindingSummaryProvider provider = new FindingSummaryProvider(repository);

    @Test
    void decided_는_accepted_와_rejected_의_합이고_total_은_전체_합이다() {
        when(repository.countBySeverityAndStatus(42L)).thenReturn(List.of(
                new SeverityStatusCount(Severity.ERROR, FindingStatus.ACCEPTED, 1L),
                new SeverityStatusCount(Severity.ERROR, FindingStatus.OPEN, 2L),
                new SeverityStatusCount(Severity.WARNING, FindingStatus.REJECTED, 1L),
                new SeverityStatusCount(Severity.WARNING, FindingStatus.OPEN, 2L),
                new SeverityStatusCount(Severity.INFO, FindingStatus.OPEN, 2L)
        ));

        JobSummaryView summary = provider.summarize(42L);

        assertThat(summary.total()).isEqualTo(8);
        assertThat(summary.bySeverity()).containsExactly(
                org.assertj.core.api.Assertions.entry("ERROR", 3L),
                org.assertj.core.api.Assertions.entry("WARNING", 3L),
                org.assertj.core.api.Assertions.entry("INFO", 2L));
        assertThat(summary.accepted()).isEqualTo(1);
        assertThat(summary.rejected()).isEqualTo(1);
        assertThat(summary.decided()).isEqualTo(2);
        assertThat(summary.open()).isEqualTo(6);
        assertThat(summary.decided() + summary.open()).isEqualTo(summary.total());
    }

    @Test
    void 항목이_없어도_bySeverity_의_키_세_개는_유지된다() {
        when(repository.countBySeverityAndStatus(42L)).thenReturn(List.of());

        JobSummaryView summary = provider.summarize(42L);

        assertThat(summary.total()).isZero();
        assertThat(summary.bySeverity()).containsOnlyKeys("ERROR", "WARNING", "INFO");
        assertThat(summary.bySeverity().values()).containsOnly(0L);
    }
}
