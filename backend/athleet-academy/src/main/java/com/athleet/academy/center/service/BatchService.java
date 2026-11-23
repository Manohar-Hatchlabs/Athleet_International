package com.athleet.academy.center.service;

import com.athleet.academy.center.domain.Batch;
import com.athleet.academy.center.domain.BatchSlot;
import com.athleet.academy.center.dto.CenterDtos.BatchRequest;
import com.athleet.academy.center.dto.CenterDtos.BatchResponse;
import com.athleet.academy.center.dto.CenterDtos.CoachRef;
import com.athleet.academy.center.dto.CenterDtos.Option;
import com.athleet.academy.center.dto.CenterDtos.SlotDto;
import com.athleet.academy.center.repository.BatchRepository;
import com.athleet.academy.common.Text;
import com.athleet.academy.student.repository.StudentRepository;
import com.athleet.core.audit.service.AuditService;
import com.athleet.core.common.error.BusinessRuleException;
import com.athleet.core.common.error.ConflictException;
import com.athleet.core.common.error.NotFoundException;
import com.athleet.core.iam.domain.AccessLevel;
import com.athleet.core.iam.domain.AppModule;
import com.athleet.core.iam.domain.AppUser;
import com.athleet.core.iam.domain.MembershipStatus;
import com.athleet.core.iam.repository.AppUserRepository;
import com.athleet.core.iam.repository.MembershipRepository;
import com.athleet.core.iam.service.AccessService;
import com.athleet.core.iam.service.OrgAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BatchService {

    private final AccessService access;
    private final CenterService centerService;
    private final BatchRepository batches;
    private final StudentRepository students;
    private final MembershipRepository memberships;
    private final AppUserRepository users;
    private final AuditService audit;

    public BatchService(AccessService access, CenterService centerService, BatchRepository batches,
                        StudentRepository students, MembershipRepository memberships, AppUserRepository users,
                        AuditService audit) {
        this.access = access;
        this.centerService = centerService;
        this.batches = batches;
        this.students = students;
        this.memberships = memberships;
        this.users = users;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<BatchResponse> list(UUID orgId, UUID centerId) {
        OrgAccess a = access.require(orgId, AppModule.CENTER, AccessLevel.VIEW);
        centerService.load(a, centerId);
        List<Batch> list = batches.findByCenterIdAndOrganizationIdOrderByNameAsc(centerId, orgId);
        Map<UUID, Long> counts = list.isEmpty() ? Map.of() : students.activeCountByBatch(list.stream().map(Batch::getId).toList());
        Map<UUID, String> names = coachNames(list.stream().flatMap(b -> b.getCoachUserIds().stream()).collect(Collectors.toSet()));
        return list.stream().map(b -> toResponse(b, names, counts.getOrDefault(b.getId(), 0L))).toList();
    }

    @Transactional(readOnly = true)
    public List<Option> options(UUID orgId, UUID centerId) {
        OrgAccess a = access.require(orgId, AppModule.CENTER, AccessLevel.VIEW);
        centerService.load(a, centerId);
        return batches.findByCenterIdAndOrganizationIdOrderByNameAsc(centerId, orgId).stream()
                .map(b -> new Option(b.getId(), b.getName(), b.getCenterId())).toList();
    }

    @Transactional
    public BatchResponse create(UUID orgId, UUID centerId, BatchRequest req) {
        OrgAccess a = access.require(orgId, AppModule.CENTER, AccessLevel.MANAGE);
        centerService.load(a, centerId);
        String name = Text.clean(req.name());
        if (batches.existsByCenterIdAndNameIgnoreCase(centerId, name)) {
            throw new ConflictException("A batch with this name already exists in this class.");
        }
        Batch b = new Batch(orgId, centerId, name);
        apply(orgId, b, req);
        batches.save(b);
        audit.record(orgId, a.userId(), "BATCH", b.getId(), "CREATE", null);
        return toResponse(b, coachNames(b.getCoachUserIds()), 0);
    }

    @Transactional
    public BatchResponse update(UUID orgId, UUID batchId, BatchRequest req) {
        OrgAccess a = access.require(orgId, AppModule.CENTER, AccessLevel.MANAGE);
        Batch b = load(a, batchId);
        String name = Text.clean(req.name());
        if (batches.existsByCenterIdAndNameIgnoreCaseAndIdNot(b.getCenterId(), name, batchId)) {
            throw new ConflictException("A batch with this name already exists in this class.");
        }
        b.setName(name);
        apply(orgId, b, req);
        audit.record(orgId, a.userId(), "BATCH", b.getId(), "UPDATE", null);
        long count = students.activeCountByBatch(List.of(batchId)).getOrDefault(batchId, 0L);
        return toResponse(b, coachNames(b.getCoachUserIds()), count);
    }

    @Transactional
    public void delete(UUID orgId, UUID batchId) {
        OrgAccess a = access.require(orgId, AppModule.CENTER, AccessLevel.MANAGE);
        Batch b = load(a, batchId);
        if (students.existsByBatchId(batchId)) {
            throw new BusinessRuleException("BATCH_IN_USE", "Move its students to another batch before deleting it.");
        }
        batches.delete(b);
        audit.record(orgId, a.userId(), "BATCH", batchId, "DELETE", null);
    }

    Batch load(OrgAccess a, UUID batchId) {
        Batch b = batches.findByIdAndOrganizationId(batchId, a.organizationId())
                .orElseThrow(() -> new NotFoundException("Batch not found."));
        a.requireCenter(b.getCenterId());
        return b;
    }

    private void apply(UUID orgId, Batch b, BatchRequest r) {
        Set<DayOfWeek> seen = EnumSet.noneOf(DayOfWeek.class);
        for (SlotDto s : r.slots()) {
            if (!seen.add(s.dayOfWeek())) {
                throw new BusinessRuleException("DUPLICATE_DAY", "Each training day can appear only once.");
            }
        }
        Set<UUID> coaches = r.coachUserIds() == null ? Set.of() : r.coachUserIds();
        if (!coaches.isEmpty()) {
            List<UUID> valid = memberships.userIdsInOrganization(orgId, MembershipStatus.ACTIVE, coaches);
            if (valid.size() != coaches.size()) {
                throw new BusinessRuleException("INVALID_COACH", "Coaches must be active staff of this academy.");
            }
        }
        b.setStartDate(r.startDate());
        b.setEndDate(r.endDate());
        b.setCapacity(r.capacity());
        b.setStatus(r.status());
        b.setDescription(Text.clean(r.description()));
        b.replaceSlots(r.slots().stream()
                .sorted(Comparator.comparing(SlotDto::dayOfWeek))
                .map(s -> new BatchSlot(s.dayOfWeek(), s.startTime(), s.endTime())).toList());
        b.replaceCoaches(coaches);
    }

    private Map<UUID, String> coachNames(Set<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return users.findByIdIn(ids).stream().collect(Collectors.toMap(AppUser::getId, AppUser::fullName, (x, y) -> x));
    }

    private static BatchResponse toResponse(Batch b, Map<UUID, String> names, long studentCount) {
        List<SlotDto> slots = b.getSlots().stream().map(s -> new SlotDto(s.getDayOfWeek(), s.getStartTime(), s.getEndTime())).toList();
        List<CoachRef> coaches = b.getCoachUserIds().stream()
                .map(id -> new CoachRef(id, names.getOrDefault(id, "")))
                .sorted(Comparator.comparing(CoachRef::name)).toList();
        return new BatchResponse(b.getId(), b.getCenterId(), b.getName(), b.getStartDate(), b.getEndDate(), slots,
                coaches, b.getCapacity(), b.getStatus(), b.getDescription(), studentCount);
    }
}
