package com.athleet.academy.center.domain;

import com.athleet.core.common.persistence.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "batch")
public class Batch extends BaseEntity {

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "center_id", nullable = false, updatable = false)
    private UUID centerId;

    @Column(nullable = false)
    private String name;

    @Column(name = "start_date") private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;

    @Column(nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActiveStatus status = ActiveStatus.ACTIVE;

    private String description;

    @ElementCollection
    @CollectionTable(name = "batch_schedule", joinColumns = @JoinColumn(name = "batch_id"))
    @OrderBy("dayOfWeek")
    private List<BatchSlot> slots = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "batch_coach", joinColumns = @JoinColumn(name = "batch_id"))
    @Column(name = "user_id")
    private Set<UUID> coachUserIds = new HashSet<>();

    protected Batch() {
    }

    public Batch(UUID organizationId, UUID centerId, String name) {
        this.organizationId = organizationId;
        this.centerId = centerId;
        this.name = name;
    }

    public void replaceSlots(List<BatchSlot> next) {
        slots.clear();
        slots.addAll(next);
    }

    public void replaceCoaches(Set<UUID> next) {
        coachUserIds.clear();
        coachUserIds.addAll(next);
    }

    public UUID getOrganizationId() { return organizationId; }
    public UUID getCenterId() { return centerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public ActiveStatus getStatus() { return status; }
    public void setStatus(ActiveStatus status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<BatchSlot> getSlots() { return slots; }
    public Set<UUID> getCoachUserIds() { return coachUserIds; }
}
