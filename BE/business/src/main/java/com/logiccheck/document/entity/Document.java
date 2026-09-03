package com.logiccheck.document.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private String title;

    @Column(name = "file_key", nullable = false, length = 512)
    private String fileKey;

    @Column(name = "file_hash", nullable = false, length = 64)
    private String fileHash;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "page_count")
    private Integer pageCount;

    // 화면 목록에 그대로 노출되는 값들. version은 재분석할 때마다 올린다.
    @Column(nullable = false)
    private int version = 1;

    @Column
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "parse_status", nullable = false, length = 20)
    private ParseStatus parseStatus = ParseStatus.PENDING;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Document() {
    }

    private Document(Long ownerId, String title, String fileKey, String fileHash, String mimeType, long sizeBytes) {
        this.ownerId = ownerId;
        this.title = title;
        this.fileKey = fileKey;
        this.fileHash = fileHash;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.parseStatus = ParseStatus.PENDING;
    }

    public static Document upload(Long ownerId, String title, String fileKey, String fileHash, String mimeType,
                                   long sizeBytes) {
        return new Document(ownerId, title, fileKey, fileHash, mimeType, sizeBytes);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        uploadedAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void markParsing() {
        this.parseStatus = ParseStatus.PARSING;
    }

    public void markExtracting() {
        this.parseStatus = ParseStatus.EXTRACTING;
    }

    public void completeParsing(int pageCount) {
        this.pageCount = pageCount;
        this.parseStatus = ParseStatus.DONE;
    }

    public void describe(String summary) {
        this.summary = summary;
    }

    public void bumpVersion() {
        this.version += 1;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    public void markFailed() {
        this.parseStatus = ParseStatus.FAILED;
    }

    public void rename(String title) {
        this.title = title;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getTitle() {
        return title;
    }

    public String getFileKey() {
        return fileKey;
    }

    public String getFileHash() {
        return fileHash;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public int getVersion() {
        return version;
    }

    public String getSummary() {
        return summary;
    }

    public ParseStatus getParseStatus() {
        return parseStatus;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
