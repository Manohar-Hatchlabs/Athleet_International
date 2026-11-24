package com.athleet.core.storage.web;

import com.athleet.core.storage.domain.FilePurpose;
import com.athleet.core.storage.dto.FileDtos.Download;
import com.athleet.core.storage.dto.FileDtos.FileResponse;
import com.athleet.core.storage.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Files")
public class FileController {

    private final FileService files;

    public FileController(FileService files) {
        this.files = files;
    }

    @Operation(summary = "Upload a picture (JPEG or PNG, up to 5 MB). Use the returned id when punching in or out.")
    @PostMapping("/orgs/{orgId}/files")
    @ResponseStatus(HttpStatus.CREATED)
    public FileResponse upload(@PathVariable UUID orgId, @RequestParam("purpose") FilePurpose purpose,
                               @RequestParam("file") MultipartFile file) {
        return files.upload(orgId, purpose, file);
    }

    /** Public on purpose: the HMAC signature and expiry in the link are the authorization (image tags cannot send a bearer token). */
    @Operation(summary = "Download through a short-lived signed link")
    @GetMapping("/files/signed/{fileId}")
    public ResponseEntity<InputStreamResource> signed(@PathVariable UUID fileId, @RequestParam long exp, @RequestParam String sig) {
        Download d = files.openSigned(fileId, exp, sig);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(d.contentType()))
                .contentLength(d.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename("picture.jpg").build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate())
                .body(new InputStreamResource(d.content()));
    }
}
