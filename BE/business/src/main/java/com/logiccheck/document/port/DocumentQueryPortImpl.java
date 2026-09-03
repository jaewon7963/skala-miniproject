package com.logiccheck.document.port;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.logiccheck.document.entity.Document;
import com.logiccheck.document.repository.DocumentRepository;

@Component
public class DocumentQueryPortImpl implements DocumentQueryPort {

    private final DocumentRepository documentRepository;

    public DocumentQueryPortImpl(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Override
    public Optional<DocumentMetaView> findMetaForOwner(Long documentId, Long userId) {
        return documentRepository.findByIdAndOwnerIdAndDeletedAtIsNull(documentId, userId)
                .map(this::toView);
    }

    private DocumentMetaView toView(Document d) {
        return new DocumentMetaView(d.getId(), d.getOwnerId(), d.getTitle(), d.getPageCount(),
                d.getParseStatus().name());
    }
}
