package com.athleet.academy.center.repository;

import com.athleet.academy.center.domain.Batch;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BatchRepository extends JpaRepository<Batch, UUID> {

    @EntityGraph(attributePaths = {"slots", "coachUserIds"})
    List<Batch> findByCenterIdAndOrganizationIdOrderByNameAsc(UUID centerId, UUID organizationId);

    @EntityGraph(attributePaths = {"slots", "coachUserIds"})
    Optional<Batch> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<Batch> findByIdIn(Collection<UUID> ids);

    boolean existsByCenterId(UUID centerId);

    boolean existsByCenterIdAndNameIgnoreCase(UUID centerId, String name);

    boolean existsByCenterIdAndNameIgnoreCaseAndIdNot(UUID centerId, String name, UUID id);

    /** [centerId, batchCount, capacitySum] for the given centers. */
    @Query("select b.centerId, count(b), coalesce(sum(b.capacity), 0) from Batch b where b.centerId in :centerIds group by b.centerId")
    List<Object[]> statsByCenter(Collection<UUID> centerIds);
}
