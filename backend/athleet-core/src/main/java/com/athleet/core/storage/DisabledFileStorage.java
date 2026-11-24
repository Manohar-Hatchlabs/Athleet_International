package com.athleet.core.storage;

import com.athleet.core.common.error.BusinessRuleException;
import com.athleet.core.common.error.NotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/** Default when no storage is configured: uploads are refused with a clear error. */
@Component
@ConditionalOnProperty(name = "athleet.storage.provider", havingValue = "none", matchIfMissing = true)
public class DisabledFileStorage implements FileStorage {

    @Override
    public void put(String key, byte[] bytes) {
        throw new BusinessRuleException("UPLOADS_DISABLED", "File uploads are not enabled on this server.");
    }

    @Override
    public InputStream open(String key) {
        throw new NotFoundException("File not found.");
    }

    @Override
    public void delete(String key) {
        // nothing is ever stored
    }
}
