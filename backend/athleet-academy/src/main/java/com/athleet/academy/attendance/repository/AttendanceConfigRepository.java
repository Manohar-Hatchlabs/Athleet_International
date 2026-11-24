package com.athleet.academy.attendance.repository;

import com.athleet.academy.attendance.domain.AttendanceConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AttendanceConfigRepository extends JpaRepository<AttendanceConfig, UUID> {

    Optional<AttendanceConfig> findByOrganizationId(UUID organizationId);
}
