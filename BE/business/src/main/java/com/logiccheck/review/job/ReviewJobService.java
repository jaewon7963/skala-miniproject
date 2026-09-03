package com.logiccheck.review.job;

import com.logiccheck.document.port.DocumentQueryPort;
import com.logiccheck.document.port.DocumentQueryPort.DocumentMetaView;
import com.logiccheck.global.exception.ErrorCode;
import com.logiccheck.review.support.BusinessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewJobService {

    /** documents.parse_status 가 이 값일 때만 분석을 시작할 수 있다 (DEV3 D-2). */
    private static final String PARSE_DONE = "DONE";

    private final ReviewJobRepository reviewJobRepository;
    private final DocumentQueryPort documentQueryPort;

    public ReviewJobService(ReviewJobRepository reviewJobRepository, DocumentQueryPort documentQueryPort) {
        this.reviewJobRepository = reviewJobRepository;
        this.documentQueryPort = documentQueryPort;
    }

    /**
     * 명세 16. 선행 조건 세 가지를 모두 DocumentQueryPort 로 확인한다.
     * documents 테이블을 직접 조회하지 않는다 (DEV3 D-2).
     */
    @Transactional
    public ReviewJob start(Long documentId, Long userId) {
        DocumentMetaView meta = requireOwnedDocument(documentId, userId);

        if (!PARSE_DONE.equals(meta.parseStatus())) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_READY);
        }
        if (reviewJobRepository.existsByDocumentIdAndStatusIn(documentId, JobStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.JOB_ALREADY_RUNNING);
        }

        try {
            // 애플리케이션 검사만으로는 동시 요청을 막을 수 없다.
            // ux_review_jobs_active 부분 유니크 인덱스가 최종 방어선이므로 즉시 flush 해
            // 제약 위반을 여기서 409 로 변환한다 (DEV3 D-2).
            return reviewJobRepository.saveAndFlush(ReviewJob.pending(documentId));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.JOB_ALREADY_RUNNING);
        }
    }

    /** 명세 17. */
    @Transactional(readOnly = true)
    public JobWithDocument findForOwner(Long jobId, Long userId) {
        ReviewJob job = reviewJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return new JobWithDocument(job, requireOwnedDocument(job.getDocumentId(), userId));
    }

    /** 명세 18. Job 이 없으면 404 NO_REVIEW_JOB — FE 가 업로드 화면으로 보낸다. */
    @Transactional(readOnly = true)
    public JobWithDocument findLatestForOwner(Long documentId, Long userId) {
        DocumentMetaView meta = requireOwnedDocument(documentId, userId);
        ReviewJob job = reviewJobRepository.findFirstByDocumentIdOrderByIdDesc(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_REVIEW_JOB));
        return new JobWithDocument(job, meta);
    }

    /**
     * DocumentQueryPort 는 소유자가 아니거나 soft delete 상태면 Optional.empty() 를 돌려준다.
     * 두 경우를 구분할 수 없으므로 DEV3 D-2 의 소유자 조건에 맞춰 403 으로 통일한다.
     */
    private DocumentMetaView requireOwnedDocument(Long documentId, Long userId) {
        return documentQueryPort.findMetaForOwner(documentId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
    }

    public record JobWithDocument(ReviewJob job, DocumentMetaView document) {
    }
}
