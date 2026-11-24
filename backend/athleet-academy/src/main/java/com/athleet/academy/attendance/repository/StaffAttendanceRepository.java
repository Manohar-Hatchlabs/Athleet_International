package com.athleet.academy.attendance.repository;

import com.athleet.academy.attendance.domain.StaffAttendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffAttendanceRepository extends JpaRepository<StaffAttendance, UUID> {

    Optional<StaffAttendance> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<StaffAttendance> findFirstByOrganizationIdAndUserIdAndInClientId(UUID orgId, UUID userId, UUID clientId);

    Optional<StaffAttendance> findFirstByOrganizationIdAndUserIdAndOutClientId(UUID orgId, UUID userId, UUID clientId);

    Optional<StaffAttendance> findFirstByOrganizationIdAndUserIdAndOutAtIsNullOrderByInAtDesc(UUID orgId, UUID userId);

    boolean existsByOrganizationIdAndUserIdAndWorkDateAndOutAtIsNull(UUID orgId, UUID userId, LocalDate workDate);

    List<StaffAttendance> findByOrganizationIdAndUserIdAndWorkDateBetweenOrderByInAtAsc(UUID orgId, UUID userId, LocalDate from, LocalDate to);

    List<StaffAttendance> findByOrganizationIdInAndWorkDateOrderByInAtAsc(Collection<UUID> orgIds, LocalDate workDate);

    List<StaffAttendance> findByOrganizationIdAndUserIdInAndWorkDateBetweenOrderByInAtAsc(UUID orgId, Collection<UUID> userIds, LocalDate from, LocalDate to);

    @Query("""
            select a from StaffAttendance a where a.organizationId = :orgId
              and (:centerId is null or a.centerId = :centerId)
              and (:userId is null or a.userId = :userId)
              and a.workDate >= :from and a.workDate <= :to
            """)
    Page<StaffAttendance> search(UUID orgId, UUID centerId, UUID userId, LocalDate from, LocalDate to, Pageable pageable);
}
