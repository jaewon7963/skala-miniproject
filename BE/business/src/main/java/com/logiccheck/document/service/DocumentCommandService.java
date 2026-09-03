package com.logiccheck.document.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.logiccheck.document.dto.DocumentDetailResponse;
import com.logiccheck.document.dto.DocumentUpdateRequest;
import com.logiccheck.document.entity.Document;
import com.logiccheck.document.entity.DocumentTag;
import com.logiccheck.document.parse.DocumentUploadedEvent;
import com.logiccheck.document.repository.DocumentRepository;
import com.logiccheck.document.repository.DocumentTagRepository;
import com.logiccheck.document.storage.LocalFileStorageService;
import com.logiccheck.global.exception.BusinessException;
import com.logiccheck.global.exception.ErrorCode;
import com.logiccheck.tag.dto.TagResponse;
import com.logiccheck.tag.entity.Tag;
import com.logiccheck.tag.repository.TagRepository;

@Service
@Transactional
public class DocumentCommandService {

    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46, 0x2D}; // "%PDF-"

    private final DocumentRepository documentRepository;
    private final DocumentTagRepository documentTagRepository;
    private final TagRepository tagRepository;
    private final DocumentQueryService documentQueryService;
    private final LocalFileStorageService storage;
    private final ApplicationEventPublisher eventPublisher;
    private final long maxUploadSizeBytes;

    public DocumentCommandService(DocumentRepository documentRepository, DocumentTagRepository documentTagRepository,
                                   TagRepository tagRepository, DocumentQueryService documentQueryService,
                                   LocalFileStorageService storage, ApplicationEventPublisher eventPublisher,
                                   @Value("${document.upload.max-size-bytes}") long maxUploadSizeBytes) {
        this.documentRepository = documentRepository;
        this.documentTagRepository = documentTagRepository;
        this.tagRepository = tagRepository;
        this.documentQueryService = documentQueryService;
        this.storage = storage;
        this.eventPublisher = eventPublisher;
        this.maxUploadSizeBytes = maxUploadSizeBytes;
    }

    public DocumentDetailResponse upload(Long ownerId, MultipartFile file) {
        byte[] content = readAll(file);

        if (!startsWithPdfMagic(content)) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
        if (content.length > maxUploadSizeBytes) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        String hash = sha256(content);
        if (documentRepository.existsByOwnerIdAndFileHashAndDeletedAtIsNull(ownerId, hash)) {
            throw new BusinessException(ErrorCode.DUPLICATE_FILE);
        }

        String fileKey = storage.store(new ByteArrayInputStream(content));
        String title = stripPdfExtension(file.getOriginalFilename());

        Document document = Document.upload(ownerId, title, fileKey, hash, "application/pdf", content.length);
        try {
            documentRepository.saveAndFlush(document);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_FILE);
        }

        eventPublisher.publishEvent(new DocumentUploadedEvent(document.getId()));
        return DocumentDetailResponse.of(document, "PARSE_PENDING", List.of());
    }

    public DocumentDetailResponse update(Long ownerId, Long documentId, DocumentUpdateRequest request) {
        Document document = documentQueryService.getOwnedOrThrow(documentId, ownerId);

        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, Map.of("field", "title"));
            }
            document.rename(request.title());
        }
        List<TagResponse> tags;
        if (request.tagIds() != null) {
            tags = replaceTags(document, request.tagIds());
        } else {
            tags = documentTagRepository.findByDocument_Id(documentId).stream()
                    .map(dt -> TagResponse.from(dt.getTag()))
                    .toList();
        }

        String displayStatus = DisplayStatusCalculator.calculate(document.getParseStatus(), null);
        return DocumentDetailResponse.of(document, displayStatus, tags);
    }

    public void delete(Long ownerId, Long documentId) {
        Document document = documentQueryService.getOwnedOrThrow(documentId, ownerId);
        document.softDelete();
    }

    private List<TagResponse> replaceTags(Document document, List<String> tagIds) {
        List<Long> ids = tagIds.stream().map(Long::valueOf).toList();
        List<Tag> found = tagRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, Map.of("field", "tagIds"));
        }

        documentTagRepository.deleteByDocument_Id(document.getId());
        documentTagRepository.flush();
        List<DocumentTag> links = found.stream().map(tag -> DocumentTag.of(document, tag)).toList();
        documentTagRepository.saveAll(links);

        return found.stream().map(TagResponse::from).toList();
    }

    private byte[] readAll(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, Map.of("field", "file"));
        }
    }

    private boolean startsWithPdfMagic(byte[] content) {
        if (content.length < PDF_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < PDF_MAGIC.length; i++) {
            if (content[i] != PDF_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    private String sha256(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String stripPdfExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "제목 없음";
        }
        return filename.toLowerCase().endsWith(".pdf") ? filename.substring(0, filename.length() - 4) : filename;
    }
}
