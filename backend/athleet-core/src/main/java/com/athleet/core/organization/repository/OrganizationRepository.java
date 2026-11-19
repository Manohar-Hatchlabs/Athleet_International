package com.athleet.core.organization.repository;

import com.athleet.core.organization.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    List<Organization> findByParentIdOrderByNameAsc(UUID parentId);

    List<Organization> findByParentIdInOrderByNameAsc(Collection<UUID> parentIds);

    List<Organization> findByParentIdIsNullOrderByNameAsc();

    boolean existsByParentIdAndNameIgnoreCase(UUID parentId, String name);

    boolean existsByParentIdIsNullAndNameIgnoreCase(String name);
}
