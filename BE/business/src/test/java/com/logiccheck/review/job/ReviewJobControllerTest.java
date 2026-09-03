package com.logiccheck.review.job;

import com.logiccheck.document.port.DocumentQueryPort.DocumentMetaView;
import com.logiccheck.global.exception.ErrorCode;
import com.logiccheck.review.job.ReviewJobService.JobWithDocument;
import com.logiccheck.review.support.BusinessException;
import com.logiccheck.review.support.CurrentUserArgumentResolver;
import com.logiccheck.review.support.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 명세 16 · 17 · 18 의 Method · Path · Status Code 와 응답 계약. */
class ReviewJobControllerTest {

    private static final DocumentMetaView META =
            new DocumentMetaView(1L, 7L, "스텁 문서", 21, "DONE");

    private ReviewJobService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(ReviewJobService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ReviewJobController(service))
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 분석_시작은_202_와_Location_헤더를_반환한다() throws Exception {
        ReviewJob job = pendingJob(42L);
        when(service.start(1L, 7L)).thenReturn(job);
        when(service.findForOwner(42L, 7L)).thenReturn(new JobWithDocument(job, META));

        mockMvc.perform(post("/api/review-jobs")
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("{\"documentId\":\"1\"}"))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "/api/review-jobs/42"))
                .andExpect(jsonPath("$.id").value("42"))
                .andExpect(jsonPath("$.documentId").value("1"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.reviewStatus").value("IN_REVIEW"))
                .andExpect(jsonPath("$.terminal").value(false))
                .andExpect(jsonPath("$.startedAt").doesNotExist())
                .andExpect(jsonPath("$.finishedAt").doesNotExist())
                .andExpect(jsonPath("$.rulesetVersion").doesNotExist())
                .andExpect(jsonPath("$.summary").doesNotExist());
    }

    @Test
    void 응답은_래퍼로_감싸지_않고_명세에_없는_필드를_담지_않는다() throws Exception {
        ReviewJob job = pendingJob(42L);
        when(service.findForOwner(42L, 7L)).thenReturn(new JobWithDocument(job, META));

        mockMvc.perform(get("/api/review-jobs/42").header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.documentTitle").value("스텁 문서"))
                .andExpect(jsonPath("$.pageCount").value(21))
                // DEV3 D-3 응답 제외 항목
                .andExpect(jsonPath("$.reviewScore").doesNotExist())
                .andExpect(jsonPath("$.steps").doesNotExist())
                .andExpect(jsonPath("$.parseProgress").doesNotExist())
                .andExpect(jsonPath("$.analyzeProgress").doesNotExist())
                .andExpect(jsonPath("$.partialFailures").doesNotExist())
                .andExpect(jsonPath("$.discovered").doesNotExist());
    }

    @Test
    void 문서가_파싱중이면_409_DOCUMENT_NOT_READY_다() throws Exception {
        when(service.start(anyLong(), anyLong()))
                .thenThrow(new BusinessException(ErrorCode.DOCUMENT_NOT_READY));

        mockMvc.perform(post("/api/review-jobs")
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("{\"documentId\":\"1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DOCUMENT_NOT_READY"))
                .andExpect(jsonPath("$.message").value("문서 파싱이 완료되지 않았습니다."));
    }

    @Test
    void 이미_진행중이면_409_JOB_ALREADY_RUNNING_이다() throws Exception {
        when(service.start(anyLong(), anyLong()))
                .thenThrow(new BusinessException(ErrorCode.JOB_ALREADY_RUNNING));

        mockMvc.perform(post("/api/review-jobs")
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("{\"documentId\":\"1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("JOB_ALREADY_RUNNING"));
    }

    @Test
    void 인증_헤더가_없으면_401_이다() throws Exception {
        mockMvc.perform(get("/api/review-jobs/42"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 분석하지_않은_문서의_최근_작업_조회는_404_NO_REVIEW_JOB_이다() throws Exception {
        when(service.findLatestForOwner(1L, 7L))
                .thenThrow(new BusinessException(ErrorCode.NO_REVIEW_JOB));

        mockMvc.perform(get("/api/documents/1/review-jobs/latest").header("X-User-Id", "7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_REVIEW_JOB"));
    }

    @Test
    void 최근_작업_조회는_200_과_Job_을_반환한다() throws Exception {
        ReviewJob job = pendingJob(42L);
        when(service.findLatestForOwner(1L, 7L)).thenReturn(new JobWithDocument(job, META));

        mockMvc.perform(get("/api/documents/1/review-jobs/latest").header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("42"));
    }

    @Test
    void documentId_가_숫자가_아니면_400_이다() throws Exception {
        mockMvc.perform(post("/api/review-jobs")
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("{\"documentId\":\"doc-1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void documentId_가_비어_있으면_400_이다() throws Exception {
        mockMvc.perform(post("/api/review-jobs")
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("{\"documentId\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private static ReviewJob pendingJob(long id) {
        ReviewJob job = ReviewJob.pending(1L);
        ReflectionTestUtils.setField(job, "id", id);
        return job;
    }
}
