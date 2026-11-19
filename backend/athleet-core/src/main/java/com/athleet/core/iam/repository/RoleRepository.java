package com.athleet.core.iam.repository;

import com.athleet.core.iam.domain.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findWithPermissionsById(UUID id);

    Optional<Role> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findWithPermissionsByIdAndOrganizationId(UUID id, UUID organizationId);

    @Query("""
            select r from Role r where r.organizationId = :orgId
              and (:search is null or lower(r.name) like :search)
            """)
    Page<Role> search(UUID orgId, String search, Pageable pageable);

    List<Role> findByIdIn(Collection<UUID> ids);

    boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);

    boolean existsByOrganizationIdAndNameIgnoreCaseAndIdNot(UUID organizationId, String name, UUID id);
}
