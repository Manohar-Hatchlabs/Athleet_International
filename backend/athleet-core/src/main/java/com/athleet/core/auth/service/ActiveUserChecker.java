package com.athleet.core.auth.service;

import com.athleet.core.common.security.UserStatusChecker;
import com.athleet.core.iam.domain.UserStatus;
import com.athleet.core.iam.repository.AppUserRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/** Cached for 30 s so deactivated users lose access quickly without a DB hit on every request. */
@Component
public class ActiveUserChecker implements UserStatusChecker {

    private final AppUserRepository users;
    private final Cache<UUID, Boolean> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(30)).maximumSize(50_000).build();

    public ActiveUserChecker(AppUserRepository users) {
        this.users = users;
    }

    @Override
    public boolean isActive(UUID userId) {
        return cache.get(userId, id -> users.findStatusById(id).map(s -> s == UserStatus.ACTIVE).orElse(false));
    }

    public void evict(UUID userId) {
        cache.invalidate(userId);
    }
}
