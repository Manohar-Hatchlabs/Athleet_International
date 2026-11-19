package com.athleet.core.iam.repository;

import com.athleet.core.iam.domain.Membership;
import com.athleet.core.iam.domain.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    List<Membership> findByUserIdAndStatus(UUID userId, MembershipStatus status);

    Optional<Membership> findByUserIdAndOrganizationIdAndStatus(UUID userId, UUID organizationId, MembershipStatus status);

    Optional<Membership> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    Optional<Membership> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByRoleId(UUID roleId);

    long countByOrganizationIdAndStatus(UUID organizationId, MembershipStatus status);

    @Query("select count(m) > 0 from Membership m where m.centerId = :centerId and m.status = :status")
    boolean anyForCenter(UUID centerId, MembershipStatus status);

    @Query("select m.userId from Membership m where m.organizationId = :orgId and m.status = :status and m.userId in :userIds")
    List<UUID> userIdsInOrganization(UUID orgId, MembershipStatus status, java.util.Collection<UUID> userIds);
}
