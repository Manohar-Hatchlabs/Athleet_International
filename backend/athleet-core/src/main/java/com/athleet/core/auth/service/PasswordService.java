package com.athleet.core.auth.service;

import com.athleet.core.common.error.BusinessRuleException;
import com.athleet.core.common.error.TooManyRequestsException;
import com.athleet.core.common.error.UnauthorizedException;
import com.athleet.core.common.security.RateLimiter;
import com.athleet.core.common.security.SecurityProperties;
import com.athleet.core.common.security.Tokens;
import com.athleet.core.iam.domain.AppUser;
import com.athleet.core.iam.domain.PasswordToken;
import com.athleet.core.iam.domain.UserStatus;
import com.athleet.core.iam.repository.AppUserRepository;
import com.athleet.core.iam.repository.PasswordTokenRepository;
import com.athleet.core.notification.service.MessageSender;
import com.athleet.core.notification.service.OutboundMessage;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** Invites, forgot-password and change-password. Links carry the token in the URL fragment so it never reaches server logs. */
@Service
public class PasswordService {

    private final AppUserRepository users;
    private final PasswordTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final MessageSender sender;
    private final UserLookup lookup;
    private final RateLimiter rateLimiter;
    private final AuthService authService;
    private final SecurityProperties props;

    public PasswordService(AppUserRepository users, PasswordTokenRepository tokens, PasswordEncoder encoder,
                           MessageSender sender, UserLookup lookup, RateLimiter rateLimiter,
                           AuthService authService, SecurityProperties props) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.sender = sender;
        this.lookup = lookup;
        this.rateLimiter = rateLimiter;
        this.authService = authService;
        this.props = props;
    }

    /** Sends a "set your password" link to a newly added staff member. */
    @Transactional
    public void sendInvite(AppUser user, String organizationName) {
        String link = createLink(user.getId());
        sender.send(new OutboundMessage(user.getEmail(), user.getMobile(),
                "You have been added to " + organizationName + " on Athleet",
                "Hello " + user.getFirstName() + ",\n\nSet your password to sign in: " + link
                        + "\n\nThis link expires in " + props.passwordTokenTtl().toHours() + " hours."));
    }

    /** Always succeeds from the caller's point of view, so it cannot be used to discover accounts. */
    @Transactional
    public void forgotPassword(String identifier, String clientIp) {
        if (!rateLimiter.tryPerMinute("forgot-ip:" + clientIp, 5)
                || !rateLimiter.tryPerMinute("forgot-id:" + identifier.trim().toLowerCase(), 3)) {
            throw new TooManyRequestsException("Too many requests. Please wait a minute and try again.");
        }
        lookup.byIdentifier(identifier)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .ifPresent(u -> sender.send(new OutboundMessage(u.getEmail(), u.getMobile(),
                        "Reset your Athleet password",
                        "Hello " + u.getFirstName() + ",\n\nReset your password: " + createLink(u.getId())
                                + "\n\nIf you did not ask for this, ignore this message.")));
    }

    @Transactional
    public void setPassword(String rawToken, String newPassword) {
        PasswordToken token = tokens.findByTokenHash(Tokens.sha256(rawToken))
                .filter(t -> t.usable(Instant.now()))
                .orElseThrow(() -> new UnauthorizedException("This link is invalid or has expired."));
        AppUser user = users.findById(token.getUserId())
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new UnauthorizedException("This link is invalid or has expired."));
        token.markUsed();
        user.changePassword(encoder.encode(newPassword));
        authService.revokeAllSessions(user.getId());
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        AppUser user = users.findById(userId).orElseThrow(() -> new UnauthorizedException("Authentication required."));
        if (user.getPasswordHash() == null || !encoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessRuleException("WRONG_PASSWORD", "Current password is incorrect.");
        }
        user.changePassword(encoder.encode(newPassword));
        authService.revokeAllSessions(user.getId());
    }

    private String createLink(UUID userId) {
        String raw = Tokens.newToken();
        tokens.save(new PasswordToken(userId, Tokens.sha256(raw), Instant.now().plus(props.passwordTokenTtl())));
        return props.webBaseUrl() + "/set-password#token=" + raw;
    }
}
