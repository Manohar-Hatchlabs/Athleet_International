package com.athleet.core.storage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Metadata of an uploaded file. The bytes live in {@code FileStorage} under {@code storageKey}. */
@Entity
@Table(name = "stored_file")
public class StoredFile {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "storage_key", nullable = false, updatable = false)
    private String storageKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private FilePurpose purpose;

    @Column(name = "content_type", nullable = false, updatable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;

    @Column(nullable = false, updatable = false)
    private String sha256;

    @Column(name = "uploaded_by", nullable = false, updatable = false)
    private UUID uploadedBy;

    @Column(name = "attached_at")
    private Instant attachedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected StoredFile() {
    }

    public StoredFile(UUID organizationId, String storageKey, FilePurpose purpose, String contentType, long sizeBytes,
                      String sha256, UUID uploadedBy) {
        this.organizationId = organizationId;
        this.storageKey = storageKey;
        this.purpose = purpose;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
        this.uploadedBy = uploadedBy;
    }

    public void markAttached() {
        this.attachedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public String getStorageKey() { return storageKey; }
    public FilePurpose getPurpose() { return purpose; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getSha256() { return sha256; }
    public UUID getUploadedBy() { return uploadedBy; }
    public Instant getAttachedAt() { return attachedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
