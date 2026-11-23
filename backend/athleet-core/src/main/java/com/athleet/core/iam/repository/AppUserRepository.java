package com.athleet.core.iam.repository;

import com.athleet.core.iam.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    List<AppUser> findByMobile(String mobile);

    Optional<AppUser> findByCountryCodeAndMobile(String countryCode, String mobile);

    List<AppUser> findByIdIn(Collection<UUID> ids);

    @Query("select u.status from AppUser u where u.id = :id")
    Optional<com.athleet.core.iam.domain.UserStatus> findStatusById(UUID id);
}
