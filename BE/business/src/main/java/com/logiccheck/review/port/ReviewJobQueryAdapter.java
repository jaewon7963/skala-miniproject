package com.logiccheck.review.port;

import com.logiccheck.review.job.ReviewJob;
import com.logiccheck.review.job.ReviewJobRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Port ③ 구현. 개발자2가 문서 목록의 displayStatus · counts 계산에 쓴다 (DEV3 E-1). */
@Component
public class ReviewJobQueryAdapter implements ReviewJobQueryPort {

    private final ReviewJobRepository reviewJobRepository;

    public ReviewJobQueryAdapter(ReviewJobRepository reviewJobRepository) {
        this.reviewJobRepository = reviewJobRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, LatestJobView> findLatestByDocumentIds(Collection<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return Map.of();
        }

        List<ReviewJob> latest = reviewJobRepository.findLatestOfEachDocument(documentIds);

        Map<Long, LatestJobView> result = new LinkedHashMap<>();
        for (ReviewJob job : latest) {
            result.put(job.getDocumentId(), new LatestJobView(
                    job.getId(),
                    job.getDocumentId(),
                    job.getStatus().name(),
                    job.getReviewStatus().name()
            ));
        }
        return result;
    }
}
