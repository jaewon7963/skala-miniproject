package com.logiccheck.review.annotation;

import com.logiccheck.review.annotation.dto.AnnotationResponse;
import com.logiccheck.review.annotation.dto.CreateAnnotationRequest;
import com.logiccheck.review.annotation.dto.UpdateAnnotationRequest;
import com.logiccheck.review.support.CurrentUser;
import com.logiccheck.review.support.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 명세 24 · 25 · 26 · 27. */
@RestController
@RequestMapping("/api")
public class AnnotationController {

    private final AnnotationService annotationService;

    public AnnotationController(AnnotationService annotationService) {
        this.annotationService = annotationService;
    }

    /** 24. 주석 목록. */
    @GetMapping("/review-jobs/{jobId}/annotations")
    public List<AnnotationResponse> list(@PathVariable Long jobId, @CurrentUser UserPrincipal user) {
        return annotationService.findAllOfJob(jobId, user.userId()).stream()
                .map(AnnotationResponse::of)
                .toList();
    }

    /** 25. 주석 생성. */
    @PostMapping("/review-jobs/{jobId}/annotations")
    public ResponseEntity<AnnotationResponse> create(@PathVariable Long jobId,
                                                     @Valid @RequestBody CreateAnnotationRequest request,
                                                     @CurrentUser UserPrincipal user) {
        Annotation created = annotationService.create(jobId, user.userId(), request.findingId(),
                request.body(), request.anchor() == null ? null : request.anchor().toAnchor());
        return ResponseEntity.status(HttpStatus.CREATED).body(AnnotationResponse.of(created));
    }

    /** 26. 주석 수정. */
    @PatchMapping("/annotations/{annotationId}")
    public AnnotationResponse update(@PathVariable Long annotationId,
                                     @Valid @RequestBody UpdateAnnotationRequest request,
                                     @CurrentUser UserPrincipal user) {
        return AnnotationResponse.of(
                annotationService.updateBody(annotationId, user.userId(), request.body()));
    }

    /** 27. 주석 삭제 (soft). */
    @DeleteMapping("/annotations/{annotationId}")
    public ResponseEntity<Void> delete(@PathVariable Long annotationId, @CurrentUser UserPrincipal user) {
        annotationService.delete(annotationId, user.userId());
        return ResponseEntity.noContent().build();
    }
}
