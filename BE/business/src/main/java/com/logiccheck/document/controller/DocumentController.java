package com.logiccheck.document.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.logiccheck.document.dto.DocumentListResponse;
import com.logiccheck.document.dto.DocumentRenameRequest;
import com.logiccheck.document.dto.DocumentResponse;
import com.logiccheck.document.dto.DocumentUpdateRequest;
import com.logiccheck.document.dto.PageResponse;
import com.logiccheck.document.dto.ParseStatusResponse;
import com.logiccheck.document.dto.SectionResponse;
import com.logiccheck.document.entity.Document;
import com.logiccheck.document.service.DocumentCommandService;
import com.logiccheck.document.service.DocumentQueryService;
import com.logiccheck.document.storage.LocalFileStorageService;
import com.logiccheck.global.security.CurrentUser;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentQueryService documentQueryService;
    private final DocumentCommandService documentCommandService;
    private final LocalFileStorageService storage;

    public DocumentController(DocumentQueryService documentQueryService,
                               DocumentCommandService documentCommandService, LocalFileStorageService storage) {
        this.documentQueryService = documentQueryService;
        this.documentCommandService = documentCommandService;
        this.storage = storage;
    }

    /**
     * 라이브러리 목록.
     *
     * <p>화면은 상태 탭이 "전체"일 때도 {@code status=ALL} 을 항상 보낸다. 여기서 400을 던지면
     * 목록이 통째로 비므로, 모르는 값이 와도 거절하지 않고 전체로 취급한다.
     * {@code tag} 는 태그 코드·이름 어느 쪽으로 와도 받는다.
     */
    @GetMapping
    public DocumentListResponse list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "ALL") String period,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "UPDATED_DESC") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUser Long ownerId) {
        return documentQueryService.list(ownerId, status, q, period, tag, sort, page, size);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(@RequestParam("file") MultipartFile file, @CurrentUser Long ownerId) {
        return documentCommandService.upload(ownerId, file);
    }

    @GetMapping("/{documentId}")
    public DocumentResponse detail(@PathVariable Long documentId, @CurrentUser Long ownerId) {
        return documentQueryService.detail(ownerId, documentId);
    }

    @PatchMapping("/{documentId}/name")
    public DocumentResponse rename(@PathVariable Long documentId, @RequestBody DocumentRenameRequest body,
                                    @CurrentUser Long ownerId) {
        return documentCommandService.rename(ownerId, documentId, body.name());
    }

    @PatchMapping("/{documentId}")
    public DocumentResponse update(@PathVariable Long documentId, @RequestBody DocumentUpdateRequest body,
                                    @CurrentUser Long ownerId) {
        return documentCommandService.update(ownerId, documentId, body);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long documentId, @CurrentUser Long ownerId) {
        documentCommandService.delete(ownerId, documentId);
    }

    @GetMapping("/{documentId}/parse-status")
    public ParseStatusResponse parseStatus(@PathVariable Long documentId, @CurrentUser Long ownerId) {
        return documentQueryService.parseStatus(ownerId, documentId);
    }

    @GetMapping("/{documentId}/file")
    public ResponseEntity<Resource> download(@PathVariable Long documentId, @CurrentUser Long ownerId) {
        Document document = documentQueryService.getForDownload(ownerId, documentId);
        Resource resource = storage.load(document.getFileKey());
        String filename = URLEncoder.encode(document.getTitle() + ".pdf", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .body(resource);
    }

    @GetMapping("/{documentId}/sections")
    public List<SectionResponse> sections(@PathVariable Long documentId, @CurrentUser Long ownerId) {
        return documentQueryService.sections(ownerId, documentId);
    }

    @GetMapping("/{documentId}/pages")
    public List<PageResponse> pages(@PathVariable Long documentId,
                                     @RequestParam(required = false) Integer from,
                                     @RequestParam(required = false) Integer to,
                                     @CurrentUser Long ownerId) {
        return documentQueryService.pages(ownerId, documentId, from, to);
    }
}
