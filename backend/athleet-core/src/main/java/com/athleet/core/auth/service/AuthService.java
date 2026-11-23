package com.athleet.core.auth.service;

import com.athleet.core.auth.domain.RefreshToken;
import com.athleet.core.auth.dto.AuthDtos.MeResponse;
import com.athleet.core.auth.repository.RefreshTokenRepository;
import com.athleet.core.common.error.NotFoundException;
import com.athleet.core.common.error.TooManyRequestsException;
import com.athleet.core.common.error.UnauthorizedException;
import com.athleet.core.common.security.AuthUser;
import com.athleet.core.common.security.JwtService;
import com.athleet.core.common.security.RateLimiter;
import com.athleet.core.common.security.SecurityProperties;
import com.athleet.core.common.security.Tokens;
import com.athleet.core.iam.domain.AppUser;
import com.athleet.core.iam.domain.UserStatus;
import com.athleet.core.iam.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_FAILURES = 5;
    private static final int LOGINS_PER_IP_PER_MINUTE = 30;

    private final UserLookup lookup;
    private final AppUserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final RateLimiter rateLimiter;
    private final SecurityProperties props;
    /** Compared against when the user does not exist, so response time does not reveal valid identifiers. */
    private final String dummyHash;

    public AuthService(UserLookup lookup, AppUserRepository users, RefreshTokenRepository refreshTokens,
                       PasswordEncoder encoder, JwtService jwt, RateLimiter rateLimiter, SecurityProperties props) {
        this.lookup = lookup;
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.jwt = jwt;
        this.rateLimiter = rateLimiter;
        this.props = props;
        this.dummyHash = encoder.encode(Tokens.newToken());
    }

    public record Session(String accessToken, long expiresIn, String refreshToken, MeResponse user) {
    }

    @Transactional
    public Session login(String identifier, String password, String clientIp) {
        if (!rateLimiter.tryPerMinute("login-ip:" + clientIp, LOGINS_PER_IP_PER_MINUTE)) {
            throw new TooManyRequestsException("Too many attempts. Please wait a minute and try again.");
        }
        String failKey = "login-fail:" + identifier.trim().toLowerCase(Locale.ROOT);
        if (rateLimiter.failures(failKey) >= MAX_FAILURES) {
            throw new TooManyRequestsException("Too many failed attempts. Try again in 15 minutes.");
        }
        Optional<AppUser> found = lookup.byIdentifier(identifier);
        AppUser user = found.orElse(null);
        String hash = user != null && user.getPasswordHash() != null ? user.getPasswordHash() : dummyHash;
        boolean ok = encoder.matches(password, hash) && user != null && user.getPasswordHash() != null
                && user.getStatus() == UserStatus.ACTIVE;
        if (!ok) {
            rateLimiter.recordFailure(failKey);
            log.info("Login failed userId={}", user != null ? user.getId() : "unknown");
            throw new UnauthorizedException("Invalid email/mobile or password.");
        }
        rateLimiter.reset(failKey);
        if (encoder.upgradeEncoding(user.getPasswordHash())) {
            user.changePassword(encoder.encode(password));
        }
        log.info("Login succeeded userId={}", user.getId());
        return issue(user);
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public Session refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException("Session expired. Please sign in again.");
        }
        RefreshToken token = refreshTokens.findByTokenHash(Tokens.sha256(rawRefreshToken))
                .orElseThrow(() -> new UnauthorizedException("Session expired. Please sign in again."));
        Instant now = Instant.now();
        if (token.isRevoked()) {
            refreshTokens.revokeAllForUser(token.getUserId(), now);
            log.warn("Refresh token reuse detected; all sessions revoked userId={}", token.getUserId());
            throw new UnauthorizedException("Session expired. Please sign in again.");
        }
        if (token.isExpired(now)) {
            throw new UnauthorizedException("Session expired. Please sign in again.");
        }
        AppUser user = users.findById(token.getUserId())
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new UnauthorizedException("Session expired. Please sign in again."));
        token.revoke();
        return issue(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;
        refreshTokens.findByTokenHash(Tokens.sha256(rawRefreshToken)).ifPresent(RefreshToken::revoke);
    }

    @Transactional(readOnly = true)
    public MeResponse me(AuthUser authUser) {
        return toMe(users.findById(authUser.id()).orElseThrow(() -> new NotFoundException("User not found.")));
    }

    @Transactional
    public void revokeAllSessions(UUID userId) {
        refreshTokens.revokeAllForUser(userId, Instant.now());
    }

    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        refreshTokens.deleteExpired(Instant.now().minus(Duration.ofDays(7)));
    }

    private Session issue(AppUser user) {
        String raw = Tokens.newToken();
        refreshTokens.save(new RefreshToken(user.getId(), Tokens.sha256(raw), Instant.now().plus(props.refreshTokenTtl())));
        String access = jwt.issueAccessToken(new AuthUser(user.getId(), user.isPlatformAdmin()));
        return new Session(access, jwt.accessTokenTtlSeconds(), raw, toMe(user));
    }

    static MeResponse toMe(AppUser u) {
        return new MeResponse(u.getId(), u.getFirstName(), u.getLastName(), u.getEmail(), u.isPlatformAdmin());
    }
}
