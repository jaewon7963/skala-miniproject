package com.logiccheck.review.port;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.logiccheck.review.job.ReviewJob;
import com.logiccheck.review.job.ReviewJobRepository;

@Component
public class ReviewJobQueryAdapter implements ReviewJobQueryPort {

    private final ReviewJobRepository reviewJobRepository;

    public ReviewJobQueryAdapter(ReviewJobRepository reviewJobRepository) {
        this.reviewJobRepository = reviewJobRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, LatestJobView> findLatestByDocumentIds(Collection<Long> documentIds) {
        if (documentIds.isEmpty()) {
            return Map.of();
        }
        return reviewJobRepository.findLatestByDocumentIds(documentIds).stream()
                .collect(Collectors.toMap(ReviewJob::getDocumentId, this::toView, (left, right) -> left));
    }

    private LatestJobView toView(ReviewJob job) {
        return new LatestJobView(job.getId(), job.getDocumentId(), job.getStatus().name(),
                job.getReviewStatus().name());
    }
}
