package com.athleet.core.auth.web;

import com.athleet.core.auth.dto.AuthDtos.ChangePasswordRequest;
import com.athleet.core.auth.dto.AuthDtos.ForgotPasswordRequest;
import com.athleet.core.auth.dto.AuthDtos.LoginRequest;
import com.athleet.core.auth.dto.AuthDtos.MeResponse;
import com.athleet.core.auth.dto.AuthDtos.SetPasswordRequest;
import com.athleet.core.auth.dto.AuthDtos.TokenResponse;
import com.athleet.core.auth.service.AuthService;
import com.athleet.core.auth.service.PasswordService;
import com.athleet.core.common.security.CurrentUser;
import com.athleet.core.common.security.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * The refresh token travels only in an HttpOnly, SameSite=Strict cookie limited to /api/v1/auth;
 * the access token is returned in the body and kept in memory by the client.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    static final String REFRESH_COOKIE = "athleet_rt";

    private final AuthService auth;
    private final PasswordService passwords;
    private final CurrentUser currentUser;
    private final SecurityProperties props;

    public AuthController(AuthService auth, PasswordService passwords, CurrentUser currentUser, SecurityProperties props) {
        this.auth = auth;
        this.passwords = passwords;
        this.currentUser = currentUser;
        this.props = props;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        return withCookie(auth.login(req.identifier(), req.password(), http.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        return withCookie(auth.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        auth.logout(refreshToken);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
    }

    @GetMapping("/me")
    public MeResponse me() {
        return auth.me(currentUser.require());
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        passwords.changePassword(currentUser.require().id(), req.currentPassword(), req.newPassword());
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest req, HttpServletRequest http) {
        passwords.forgotPassword(req.identifier(), http.getRemoteAddr());
    }

    @PostMapping("/set-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPassword(@Valid @RequestBody SetPasswordRequest req) {
        passwords.setPassword(req.token(), req.newPassword());
    }

    private ResponseEntity<TokenResponse> withCookie(AuthService.Session s) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(s.refreshToken(), props.refreshTokenTtl()).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new TokenResponse(s.accessToken(), s.expiresIn(), s.user()));
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(props.refreshCookieSecure())
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(maxAge)
                .build();
    }
}
