package com.athleet.academy.attendance.repository;

import com.athleet.academy.attendance.domain.AttendanceSession;
import com.athleet.academy.attendance.domain.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, UUID> {

    Optional<AttendanceSession> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<AttendanceSession> findByBatchIdAndSessionDate(UUID batchId, LocalDate sessionDate);

    List<AttendanceSession> findByBatchIdInAndSessionDate(Collection<UUID> batchIds, LocalDate sessionDate);

    @Query("""
            select s from AttendanceSession s where s.organizationId = :orgId
              and (:centerId is null or s.centerId = :centerId)
              and (:batchId is null or s.batchId = :batchId)
              and s.status in :statuses
              and s.sessionDate >= :from and s.sessionDate <= :to
            """)
    Page<AttendanceSession> search(UUID orgId, UUID centerId, UUID batchId, Collection<SessionStatus> statuses,
                                   LocalDate from, LocalDate to, Pageable pageable);
}
