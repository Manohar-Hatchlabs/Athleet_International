package com.athleet.core.iam.service;

import com.athleet.core.iam.domain.AppUser;
import com.athleet.core.iam.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Creates the first platform admin on an empty database, from environment variables
 * (ATHLEET_BOOTSTRAP_ADMIN_EMAIL / ATHLEET_BOOTSTRAP_ADMIN_PASSWORD). Does nothing once any user exists.
 */
@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public BootstrapAdminInitializer(AppUserRepository users, PasswordEncoder encoder,
                                     @Value("${athleet.bootstrap.admin-email:}") String email,
                                     @Value("${athleet.bootstrap.admin-password:}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) return;
        if (email.isBlank() || password.isBlank()) {
            log.warn("No users exist. Set ATHLEET_BOOTSTRAP_ADMIN_EMAIL and ATHLEET_BOOTSTRAP_ADMIN_PASSWORD to create the first admin.");
            return;
        }
        if (password.length() < 12) {
            throw new IllegalStateException("ATHLEET_BOOTSTRAP_ADMIN_PASSWORD must be at least 12 characters");
        }
        AppUser admin = new AppUser("Platform", "Admin");
        admin.setEmail(email.trim().toLowerCase(Locale.ROOT));
        admin.setPlatformAdmin(true);
        admin.changePassword(encoder.encode(password));
        users.save(admin);
        log.info("Created bootstrap platform admin userId={}", admin.getId());
    }
}
