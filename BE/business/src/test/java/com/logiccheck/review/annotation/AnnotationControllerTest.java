package com.logiccheck.review.annotation;

import com.logiccheck.global.exception.ErrorCode;
import com.logiccheck.review.job.JobStatus;
import com.logiccheck.review.job.ReviewJob;
import com.logiccheck.review.support.BusinessException;
import com.logiccheck.review.support.CurrentUserArgumentResolver;
import com.logiccheck.review.support.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 명세 24 · 25 · 26 · 27 의 Method · Path · Status Code 와 응답 계약 (DEV3 D-7). */
class AnnotationControllerTest {

    private AnnotationService service;
    private MockMvc mockMvc;
    private ReviewJob job;

    @BeforeEach
    void setUp() {
        service = mock(AnnotationService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AnnotationController(service))
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        job = ReviewJob.pending(1L);
        ReflectionTestUtils.setField(job, "id", 42L);
        ReflectionTestUtils.setField(job, "status", JobStatus.DONE);
    }

    @Test
    void 목록은_source_color_authorId_없이_반환한다() throws Exception {
        when(service.findAllOfJob(42L, 7L)).thenReturn(List.of(annotation(1L, "메모")));

        mockMvc.perform(get("/api/review-jobs/42/annotations").header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].jobId").value("42"))
                .andExpect(jsonPath("$[0].source").doesNotExist())
                .andExpect(jsonPath("$[0].color").doesNotExist())
                .andExpect(jsonPath("$[0].authorId").doesNotExist());
    }

    @Test
    void 생성은_201_이다() throws Exception {
        when(service.create(anyLong(), anyLong(), any(), anyString(), any()))
                .thenReturn(annotation(1L, "메모"));

        mockMvc.perform(post("/api/review-jobs/42/annotations")
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("{\"body\":\"메모\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("1"));
    }

    @Test
    void body_가_공백이면_400_이다() throws Exception {
        mockMvc.perform(post("/api/review-jobs/42/annotations")
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("{\"body\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details.body").exists());
    }

    @Test
    void bbox_가_0에서_1_밖이면_400_이다() throws Exception {
        mockMvc.perform(post("/api/review-jobs/42/annotations")
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("{\"body\":\"메모\",\"anchor\":{\"page\":1,"
                                + "\"bbox\":{\"x\":1.5,\"y\":0.1,\"w\":0.1,\"h\":0.1}}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details['anchor.bbox.x']").exists());
    }

    @Test
    void bbox_가_일부만_오면_400_이다() throws Exception {
        mockMvc.perform(post("/api/review-jobs/42/annotations")
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("{\"body\":\"메모\",\"anchor\":{\"page\":1,\"bbox\":{\"x\":0.1,\"y\":0.1}}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details['anchor.bbox.w']").exists())
                .andExpect(jsonPath("$.details['anchor.bbox.h']").exists());
    }

    @Test
    void 수정은_200_이다() throws Exception {
        when(service.updateBody(1L, 7L, "수정")).thenReturn(annotation(1L, "수정"));

        mockMvc.perform(patch("/api/annotations/1")
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("{\"body\":\"수정\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("수정"));
    }

    @Test
    void 삭제는_204_이고_본문이_없다() throws Exception {
        mockMvc.perform(delete("/api/annotations/1").header("X-User-Id", "7"))
                .andExpect(status().isNoContent())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string(""));
    }

    @Test
    void 이미_삭제된_주석은_404_다() throws Exception {
        doThrow(new BusinessException(ErrorCode.NOT_FOUND)).when(service).delete(1L, 7L);

        mockMvc.perform(delete("/api/annotations/1").header("X-User-Id", "7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void 타인_주석_접근은_403_이다() throws Exception {
        when(service.updateBody(anyLong(), anyLong(), anyString()))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mockMvc.perform(patch("/api/annotations/1")
                        .header("X-User-Id", "9")
                        .contentType("application/json")
                        .content("{\"body\":\"메모\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private Annotation annotation(long id, String body) {
        Annotation annotation = Annotation.create(job, null, 7L, body, null);
        ReflectionTestUtils.setField(annotation, "id", id);
        return annotation;
    }
}
