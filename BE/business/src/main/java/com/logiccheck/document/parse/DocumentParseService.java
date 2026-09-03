package com.logiccheck.document.parse;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.logiccheck.document.repository.DocumentRepository;
import com.logiccheck.document.storage.LocalFileStorageService;

@Component
public class DocumentParseService {

    private static final Logger log = LoggerFactory.getLogger(DocumentParseService.class);

    private final DocumentRepository documentRepository;
    private final LocalFileStorageService storage;
    private final PdfStructureExtractor extractor;
    private final DocumentParseStatusUpdater statusUpdater;

    public DocumentParseService(DocumentRepository documentRepository, LocalFileStorageService storage,
                                 PdfStructureExtractor extractor, DocumentParseStatusUpdater statusUpdater) {
        this.documentRepository = documentRepository;
        this.storage = storage;
        this.extractor = extractor;
        this.statusUpdater = statusUpdater;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUploaded(DocumentUploadedEvent event) {
        parse(event.documentId());
    }

    private void parse(Long documentId) {
        statusUpdater.markParsing(documentId);
        try {
            String fileKey = documentRepository.findById(documentId).orElseThrow().getFileKey();
            Path file = storage.resolve(fileKey);
            statusUpdater.markExtracting(documentId);
            ParsedDocument parsed = extractor.extract(file);
            statusUpdater.completeParsing(documentId, parsed);
        } catch (Exception e) {
            log.error("문서 {} 파싱에 실패했습니다", documentId, e);
            statusUpdater.markFailed(documentId);
        }
    }
}
