package com.athleet.core.common.security;

import com.athleet.core.common.error.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

/**
 * Deny by default: only the endpoints listed here are public. The API is stateless (Bearer JWT); the only cookie
 * is the HttpOnly SameSite=Strict refresh cookie scoped to /api/v1/auth, so CSRF protection is not needed.
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService, UserStatusChecker statusChecker,
                                            ObjectMapper mapper, SecurityProperties props,
                                            @Value("${springdoc.api-docs.enabled:false}") boolean apiDocs) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(c -> c.configurationSource(corsSource(props)))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .headers(h -> h
                        .contentSecurityPolicy(csp -> csp.policyDirectives(apiDocs
                                ? "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; frame-ancestors 'none'"
                                : "default-src 'none'; frame-ancestors 'none'"))
                        .frameOptions(f -> f.deny())
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000)))
                .authorizeHttpRequests(a -> {
                    a.requestMatchers(HttpMethod.POST,
                            "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout",
                            "/api/v1/auth/forgot-password", "/api/v1/auth/set-password").permitAll();
                    a.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll();
                    if (apiDocs) {
                        a.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                    }
                    a.anyRequest().authenticated();
                })
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                write(res, mapper, HttpServletResponse.SC_UNAUTHORIZED, ApiError.of("UNAUTHORIZED", "Authentication required.")))
                        .accessDeniedHandler((req, res, ex) ->
                                write(res, mapper, HttpServletResponse.SC_FORBIDDEN, ApiError.of("FORBIDDEN", "You do not have permission to do this."))))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, statusChecker), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    private static CorsConfigurationSource corsSource(SecurityProperties props) {
        CorsConfiguration cfg = new CorsConfiguration();
        List<String> origins = props.corsAllowedOrigins() == null ? List.of() : props.corsAllowedOrigins();
        if (origins.contains("*")) {
            throw new IllegalStateException("Wildcard CORS origin is not allowed");
        }
        cfg.setAllowedOrigins(origins);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        return source;
    }

    private static void write(HttpServletResponse res, ObjectMapper mapper, int status, ApiError body) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(res.getOutputStream(), body);
    }
}
