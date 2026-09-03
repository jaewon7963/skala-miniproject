package com.logiccheck.review.finding;

import com.logiccheck.global.exception.ErrorCode;
import com.logiccheck.review.job.JobStatus;
import com.logiccheck.review.job.ReviewJob;
import com.logiccheck.review.support.BusinessException;
import com.logiccheck.review.support.CurrentUserArgumentResolver;
import com.logiccheck.review.support.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 명세 21 · 22 의 응답 계약 (DEV3 D-4 · D-5). */
class FindingControllerTest {

    private FindingService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(FindingService.class);
        org.springframework.validation.beanvalidation.LocalValidatorFactoryBean validator =
                new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new FindingController(service))
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void 목록은_배열을_래퍼_없이_반환하고_embedding_을_담지_않는다() throws Exception {
        ReviewJob job = FindingFixtures.job(42L, JobStatus.DONE);
        when(service.findAllOfJob(42L, 7L)).thenReturn(List.of(
                FindingFixtures.deterministic(1, job, Severity.ERROR, "0.960"),
                FindingFixtures.finding(2, job, Severity.WARNING, "0.780", null)
        ));

        mockMvc.perform(get("/api/review-jobs/42/findings").header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].embedding").doesNotExist())
                .andExpect(jsonPath("$[1].embedding").doesNotExist())
                // method 파생
                .andExpect(jsonPath("$[0].method").value("DETERMINISTIC"))
                .andExpect(jsonPath("$[1].method").value("RAG"))
                // calculation 은 DETERMINISTIC 일 때만
                .andExpect(jsonPath("$[0].calculation.expression").value("3.2 + 9.6 + 24"))
                .andExpect(jsonPath("$[0].calculation.diff").value("3,200만 원"))
                .andExpect(jsonPath("$[1].calculation").doesNotExist())
                // severity / status 는 DEV3 F절의 이름을 쓴다
                .andExpect(jsonPath("$[0].severity").value("ERROR"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].type").doesNotExist())
                .andExpect(jsonPath("$[0].verdict").doesNotExist());
    }

    @Test
    void 모든_evidence_에_앵커_키_id_와_quote_가_있고_bbox_는_0에서_1_이다() throws Exception {
        ReviewJob job = FindingFixtures.job(42L, JobStatus.DONE);
        when(service.findAllOfJob(42L, 7L))
                .thenReturn(List.of(FindingFixtures.deterministic(1, job, Severity.ERROR, "0.960")));

        mockMvc.perform(get("/api/review-jobs/42/findings").header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].evidence.length()").value(1))
                .andExpect(jsonPath("$[0].evidence[0].id").value("10"))
                .andExpect(jsonPath("$[0].evidence[0].quote").value("2027년 매출 24억 원"))
                .andExpect(jsonPath("$[0].evidence[0].bbox.x").value(0.12))
                .andExpect(jsonPath("$[0].evidence[0].bbox.y").value(0.31))
                .andExpect(jsonPath("$[0].evidence[0].bbox.w").value(0.66))
                .andExpect(jsonPath("$[0].evidence[0].bbox.h").value(0.03))
                // ERD 에 Block 테이블이 없어 FE 의 anchorId 를 evidence.id 로 대체한다
                .andExpect(jsonPath("$[0].evidence[0].blockId").doesNotExist());
    }

    @Test
    void 분석_미완료_Job_은_200_과_빈_배열이다() throws Exception {
        when(service.findAllOfJob(42L, 7L)).thenReturn(List.of());

        mockMvc.perform(get("/api/review-jobs/42/findings").header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void 상세는_200_과_단일_객체를_반환한다() throws Exception {
        ReviewJob job = FindingFixtures.job(42L, JobStatus.DONE);
        when(service.findOne(1L, 7L)).thenReturn(FindingFixtures.deterministic(1, job, Severity.ERROR, "0.960"));

        mockMvc.perform(get("/api/findings/1").header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.jobId").value("42"))
                .andExpect(jsonPath("$.page").value(11))
                .andExpect(jsonPath("$.confidence").value(0.96));
    }

    @Test
    void 없는_Finding_상세는_404_다() throws Exception {
        when(service.findOne(anyLong(), anyLong())).thenThrow(new BusinessException(ErrorCode.NOT_FOUND));

        mockMvc.perform(get("/api/findings/999").header("X-User-Id", "7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void 판정은_201_과_갱신된_Finding_을_반환한다() throws Exception {
        ReviewJob job = FindingFixtures.job(42L, JobStatus.DONE);
        Finding accepted = FindingFixtures.deterministic(1, job, Severity.ERROR, "0.960");
        accepted.decide(DecisionAction.ACCEPT);
        when(service.decide(1L, 7L, DecisionAction.ACCEPT, null, null)).thenReturn(accepted);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/findings/1/decisions")
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("{\"action\":\"ACCEPT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void action_이_없으면_400_이다() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/findings/1/decisions")
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 지원하지_않는_action_은_400_이다() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/findings/1/decisions")
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("{\"action\":\"HOLD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 완료된_Job_의_Finding_판정은_409_다() throws Exception {
        when(service.decide(anyLong(), anyLong(), any(), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.JOB_ALREADY_COMPLETED));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/findings/1/decisions")
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("{\"action\":\"ACCEPT\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("JOB_ALREADY_COMPLETED"));
    }

    @Test
    void 타인_Finding_상세는_403_이다() throws Exception {
        when(service.findOne(anyLong(), anyLong())).thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mockMvc.perform(get("/api/findings/1").header("X-User-Id", "9"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
