package com.athleet.core.storage.dto;

import java.io.InputStream;
import java.util.UUID;

public final class FileDtos {

    private FileDtos() {
    }

    public record FileResponse(UUID id, String contentType, long sizeBytes) {
    }

    /** An open download. The caller closes the stream. */
    public record Download(String contentType, long sizeBytes, InputStream content) {
    }
}
