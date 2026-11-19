package com.athleet.core.auth.service;

import com.athleet.core.iam.domain.AppUser;
import com.athleet.core.iam.repository.AppUserRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Finds a user by the single "Email / Mobile" login field. */
@Component
public class UserLookup {

    private final AppUserRepository users;

    public UserLookup(AppUserRepository users) {
        this.users = users;
    }

    public Optional<AppUser> byIdentifier(String identifier) {
        String id = identifier.trim();
        if (id.contains("@")) {
            return users.findByEmailIgnoreCase(id.toLowerCase(Locale.ROOT));
        }
        String digits = id.replaceAll("\\D", "");
        if (digits.length() < 6 || digits.length() > 15) {
            return Optional.empty();
        }
        List<AppUser> matches = users.findByMobile(digits);
        if (matches.size() == 1) {
            return Optional.of(matches.get(0));
        }
        // "+91 98..." style input: try the last 10 digits as the national number.
        if (digits.length() > 10) {
            List<AppUser> tail = users.findByMobile(digits.substring(digits.length() - 10));
            if (tail.size() == 1) return Optional.of(tail.get(0));
        }
        return Optional.empty();
    }
}
