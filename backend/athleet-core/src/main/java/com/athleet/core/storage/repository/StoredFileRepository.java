package com.athleet.core.storage.repository;

import com.athleet.core.storage.domain.StoredFile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {

    Optional<StoredFile> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<StoredFile> findByAttachedAtIsNullAndCreatedAtBeforeOrderByCreatedAtAsc(Instant before, Pageable pageable);
}
