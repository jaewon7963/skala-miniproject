package com.logiccheck.review.annotation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnnotationRepository extends JpaRepository<Annotation, Long> {

    /** 명세 24. soft delete 된 행은 제외한다 (DEV3 D-7). */
    List<Annotation> findByJobIdAndDeletedAtIsNullOrderByIdAsc(Long jobId);

    Optional<Annotation> findByIdAndDeletedAtIsNull(Long id);
}
