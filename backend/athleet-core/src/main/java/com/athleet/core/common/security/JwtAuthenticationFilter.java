package com.athleet.core.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Reads "Authorization: Bearer ..." and, if the token verifies, sets the authenticated {@link AuthUser}. */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserStatusChecker statusChecker;

    public JwtAuthenticationFilter(JwtService jwtService, UserStatusChecker statusChecker) {
        this.jwtService = jwtService;
        this.statusChecker = statusChecker;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            jwtService.verify(header.substring(7).trim())
                    .filter(u -> statusChecker.isActive(u.id()))
                    .ifPresent(user -> {
                        var authorities = user.platformAdmin()
                                ? List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"))
                                : List.of(new SimpleGrantedAuthority("ROLE_USER"));
                        var auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
        }
        chain.doFilter(request, response);
    }
}
