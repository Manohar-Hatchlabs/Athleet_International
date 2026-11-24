package com.athleet.core.storage.service;

import com.athleet.core.common.error.BusinessRuleException;
import com.athleet.core.common.error.NotFoundException;
import com.athleet.core.common.error.TooManyRequestsException;
import com.athleet.core.common.security.RateLimiter;
import com.athleet.core.iam.service.AccessService;
import com.athleet.core.iam.service.StaffContext;
import com.athleet.core.storage.FileStorage;
import com.athleet.core.storage.domain.FilePurpose;
import com.athleet.core.storage.domain.StoredFile;
import com.athleet.core.storage.dto.FileDtos.Download;
import com.athleet.core.storage.dto.FileDtos.FileResponse;
import com.athleet.core.storage.repository.StoredFileRepository;
import com.athleet.core.storage.config.StorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Uploads, attaches and serves files. A file is uploaded first (unattached), then a feature record claims it with
 * {@link #attach}; unattached files older than a day are removed by {@link #cleanUpUnattached()}.
 */
@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private static final Duration ATTACH_WINDOW = Duration.ofHours(1);
    private static final Duration ORPHAN_AGE = Duration.ofHours(24);
    private static final int UPLOADS_PER_MINUTE = 20;

    private final AccessService access;
    private final FileStorage storage;
    private final StoredFileRepository files;
    private final RateLimiter rateLimiter;
    private final SignedUrls signedUrls;
    private final StorageProperties props;

    public FileService(AccessService access, FileStorage storage, StoredFileRepository files, RateLimiter rateLimiter,
                       SignedUrls signedUrls, StorageProperties props) {
        this.access = access;
        this.storage = storage;
        this.files = files;
        this.rateLimiter = rateLimiter;
        this.signedUrls = signedUrls;
        this.props = props;
    }

    @Transactional
    public FileResponse upload(UUID orgId, FilePurpose purpose, MultipartFile file) {
        StaffContext staff = access.requireStaff(orgId);
        if ("none".equals(props.provider())) {
            throw new BusinessRuleException("UPLOADS_DISABLED", "File uploads are not enabled on this server.");
        }
        if (!rateLimiter.tryPerMinute("upload:" + staff.userId(), UPLOADS_PER_MINUTE)) {
            throw new TooManyRequestsException("Too many uploads. Please wait a minute.");
        }
        byte[] raw;
        try {
            raw = file.getBytes();
        } catch (IOException e) {
            throw new BusinessRuleException("INVALID_FILE", "The upload could not be read.");
        }
        byte[] jpeg = ImageUploads.normalise(file.getOriginalFilename(), file.getContentType(), raw);

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String key = "org/" + orgId + "/" + purpose.keyPrefix() + "/" + now.getYear() + "/"
                + String.format("%02d", now.getMonthValue()) + "/" + UUID.randomUUID() + ".jpg";
        storage.put(key, jpeg);
        try {
            StoredFile saved = files.save(new StoredFile(orgId, key, purpose, ImageUploads.OUTPUT_TYPE, jpeg.length,
                    sha256(jpeg), staff.userId()));
            return new FileResponse(saved.getId(), saved.getContentType(), saved.getSizeBytes());
        } catch (RuntimeException e) {
            storage.delete(key);
            throw e;
        }
    }

    /**
     * Claims an uploaded file for a record. Runs in the caller's transaction so the claim commits with the record.
     * The file must be in the same organization, uploaded by {@code userId} for {@code purpose}, unclaimed and recent.
     */
    @Transactional
    public StoredFile attach(UUID orgId, UUID fileId, UUID userId, FilePurpose purpose) {
        StoredFile f = files.findByIdAndOrganizationId(fileId, orgId)
                .orElseThrow(() -> new BusinessRuleException("INVALID_FILE", "The uploaded picture was not found."));
        boolean fresh = f.getCreatedAt().isAfter(Instant.now().minus(ATTACH_WINDOW));
        if (!f.getUploadedBy().equals(userId) || f.getPurpose() != purpose || f.getAttachedAt() != null || !fresh) {
            throw new BusinessRuleException("INVALID_FILE", "The uploaded picture cannot be used. Upload it again.");
        }
        f.markAttached();
        return f;
    }

    /** A short-lived link the web app can put in an image tag. Callers check access to the owning record first. */
    public String downloadUrl(UUID fileId) {
        return signedUrls.create(fileId);
    }

    @Transactional(readOnly = true)
    public Download openSigned(UUID fileId, long exp, String sig) {
        if (!signedUrls.valid(fileId, exp, sig)) {
            throw new NotFoundException("File not found.");
        }
        StoredFile f = files.findById(fileId).orElseThrow(() -> new NotFoundException("File not found."));
        return new Download(f.getContentType(), f.getSizeBytes(), storage.open(f.getStorageKey()));
    }

    /** Removes files that were uploaded but never attached to a record. Safe to run repeatedly. */
    @Scheduled(cron = "0 17 * * * *")
    @Transactional
    public void cleanUpUnattached() {
        List<StoredFile> stale = files.findByAttachedAtIsNullAndCreatedAtBeforeOrderByCreatedAtAsc(
                Instant.now().minus(ORPHAN_AGE), PageRequest.of(0, 200));
        for (StoredFile f : stale) {
            storage.delete(f.getStorageKey());
            files.delete(f);
        }
        if (!stale.isEmpty()) {
            log.info("Removed {} unattached uploads", stale.size());
        }
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
