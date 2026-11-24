package com.athleet.core.storage.service;

import com.athleet.core.storage.config.StorageProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;
import java.util.UUID;

/** Short-lived HMAC-signed download links. The signature is the only authorization on the public download endpoint. */
@Component
class SignedUrls {

    private final StorageProperties props;
    private final Clock clock = Clock.systemUTC();

    SignedUrls(StorageProperties props) {
        this.props = props;
    }

    /** Relative URL of the signed download endpoint. */
    String create(UUID fileId) {
        long exp = clock.instant().plus(props.downloadUrlTtl()).getEpochSecond();
        return "/api/v1/files/signed/" + fileId + "?exp=" + exp + "&sig=" + sign(fileId, exp);
    }

    boolean valid(UUID fileId, long exp, String sig) {
        if (sig == null || exp < clock.instant().getEpochSecond() || props.urlSigningSecret() == null) {
            return false;
        }
        return MessageDigest.isEqual(sign(fileId, exp).getBytes(StandardCharsets.UTF_8), sig.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(UUID fileId, long exp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(props.urlSigningSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal((fileId + "|" + exp).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC unavailable", e);
        }
    }
}
