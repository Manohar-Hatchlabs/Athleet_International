package com.athleet.core.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/** Issues and verifies short-lived HS256 access tokens. Signature, expiry and issuer are always checked. */
@Service
public class JwtService {

    private static final String CLAIM_TYPE = "typ";
    private static final String CLAIM_PLATFORM_ADMIN = "pa";
    private static final String TYPE_ACCESS = "access";

    private final SecretKey key;
    private final SecurityProperties props;

    public JwtService(SecurityProperties props) {
        byte[] raw = Decoders.BASE64.decode(props.jwtSecret());
        if (raw.length < 32) {
            throw new IllegalStateException("athleet.security.jwt-secret must be at least 256 bits (32 bytes, base64)");
        }
        this.key = Keys.hmacShaKeyFor(raw);
        this.props = props;
    }

    public String issueAccessToken(AuthUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(props.jwtIssuer())
                .subject(user.id().toString())
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim(CLAIM_PLATFORM_ADMIN, user.platformAdmin())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.accessTokenTtl())))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public long accessTokenTtlSeconds() {
        return props.accessTokenTtl().toSeconds();
    }

    public Optional<AuthUser> verify(String token) {
        try {
            Claims c = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(props.jwtIssuer())
                    .require(CLAIM_TYPE, TYPE_ACCESS)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new AuthUser(UUID.fromString(c.getSubject()),
                    Boolean.TRUE.equals(c.get(CLAIM_PLATFORM_ADMIN, Boolean.class))));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
