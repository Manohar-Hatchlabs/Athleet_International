package com.athleet.academy.center.repository;

import com.athleet.academy.center.domain.Center;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CenterRepository extends JpaRepository<Center, UUID> {

    Optional<Center> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);

    boolean existsByOrganizationIdAndNameIgnoreCaseAndIdNot(UUID organizationId, String name, UUID id);

    @Query("""
            select c from Center c where c.organizationId = :orgId
              and (:centerId is null or c.id = :centerId)
              and (:search is null or lower(c.name) like :search or lower(coalesce(c.city, '')) like :search)
            """)
    Page<Center> search(UUID orgId, UUID centerId, String search, Pageable pageable);

    List<Center> findByIdIn(Collection<UUID> ids);

    List<Center> findByOrganizationIdInOrderByNameAsc(Collection<UUID> organizationIds);
}
