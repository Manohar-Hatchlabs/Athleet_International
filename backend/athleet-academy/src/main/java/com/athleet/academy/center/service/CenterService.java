package com.athleet.academy.center.service;

import com.athleet.academy.center.domain.Center;
import com.athleet.academy.center.dto.CenterDtos.CenterRequest;
import com.athleet.academy.center.dto.CenterDtos.CenterResponse;
import com.athleet.academy.center.dto.CenterDtos.CenterSummary;
import com.athleet.academy.center.dto.CenterDtos.Option;
import com.athleet.academy.center.repository.BatchRepository;
import com.athleet.academy.center.repository.CenterRepository;
import com.athleet.academy.common.Text;
import com.athleet.academy.student.repository.StudentRepository;
import com.athleet.core.audit.service.AuditService;
import com.athleet.core.common.error.BusinessRuleException;
import com.athleet.core.common.error.ConflictException;
import com.athleet.core.common.error.NotFoundException;
import com.athleet.core.common.paging.PageResponse;
import com.athleet.core.common.paging.Pages;
import com.athleet.core.iam.domain.AccessLevel;
import com.athleet.core.iam.domain.AppModule;
import com.athleet.core.iam.domain.MembershipStatus;
import com.athleet.core.iam.repository.MembershipRepository;
import com.athleet.core.iam.service.AccessService;
import com.athleet.core.iam.service.CenterDirectory;
import com.athleet.core.iam.service.OrgAccess;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CenterService implements CenterDirectory {

    private static final Map<String, String> SORTS = Map.of("name", "name", "status", "status", "city", "city");

    private final AccessService access;
    private final CenterRepository centers;
    private final BatchRepository batches;
    private final StudentRepository students;
    private final MembershipRepository memberships;
    private final AuditService audit;

    public CenterService(AccessService access, CenterRepository centers, BatchRepository batches,
                         StudentRepository students, MembershipRepository memberships, AuditService audit) {
        this.access = access;
        this.centers = centers;
        this.batches = batches;
        this.students = students;
        this.memberships = memberships;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsInOrganization(UUID centerId, UUID organizationId) {
        return centers.existsByIdAndOrganizationId(centerId, organizationId);
    }

    @Transactional(readOnly = true)
    public PageResponse<CenterSummary> list(UUID orgId, String search, int page, int size, String sort) {
        OrgAccess a = access.require(orgId, AppModule.CENTER, AccessLevel.VIEW);
        Page<Center> result = centers.search(orgId, a.centerId(), Text.likePattern(search),
                Pages.of(page, size, sort, SORTS, Sort.by("name").and(Sort.by("id"))));
        List<UUID> ids = result.getContent().stream().map(Center::getId).toList();
        Map<UUID, long[]> stats = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : batches.statsByCenter(ids)) {
                stats.put((UUID) row[0], new long[]{((Number) row[1]).longValue(), ((Number) row[2]).longValue()});
            }
        }
        Map<UUID, Long> headcount = ids.isEmpty() ? Map.of() : students.activeCountByCenter(ids);
        return PageResponse.of(result, c -> {
            long[] s = stats.getOrDefault(c.getId(), new long[]{0, 0});
            return new CenterSummary(c.getId(), c.getName(), c.getStatus(), c.getCity(), c.getCountry(),
                    s[0], headcount.getOrDefault(c.getId(), 0L), s[1]);
        });
    }

    /** Dropdown options, limited to the caller's center when they hold a center role. */
    @Transactional(readOnly = true)
    public List<Option> options(UUID orgId) {
        OrgAccess a = access.require(orgId, AppModule.CENTER, AccessLevel.VIEW);
        return centers.search(orgId, a.centerId(), null, Pages.of(0, Pages.MAX_SIZE, null, SORTS, Sort.by("name")))
                .map(c -> new Option(c.getId(), c.getName(), null)).getContent();
    }

    @Transactional(readOnly = true)
    public CenterResponse get(UUID orgId, UUID centerId) {
        OrgAccess a = access.require(orgId, AppModule.CENTER, AccessLevel.VIEW);
        return toResponse(load(a, centerId));
    }

    @Transactional
    public CenterResponse create(UUID orgId, CenterRequest req) {
        OrgAccess a = access.require(orgId, AppModule.CENTER, AccessLevel.MANAGE);
        if (!a.allCenters()) {
            throw new NotFoundException("Not found.");
        }
        String name = Text.clean(req.name());
        if (centers.existsByOrganizationIdAndNameIgnoreCase(orgId, name)) {
            throw new ConflictException("A class with this name already exists.");
        }
        Center c = new Center(orgId, name);
        apply(c, req);
        centers.save(c);
        audit.record(orgId, a.userId(), "CENTER", c.getId(), "CREATE", null);
        return toResponse(c);
    }

    @Transactional
    public CenterResponse update(UUID orgId, UUID centerId, CenterRequest req) {
        OrgAccess a = access.require(orgId, AppModule.CENTER, AccessLevel.MANAGE);
        Center c = load(a, centerId);
        String name = Text.clean(req.name());
        if (centers.existsByOrganizationIdAndNameIgnoreCaseAndIdNot(orgId, name, centerId)) {
            throw new ConflictException("A class with this name already exists.");
        }
        c.setName(name);
        apply(c, req);
        audit.record(orgId, a.userId(), "CENTER", c.getId(), "UPDATE", null);
        return toResponse(c);
    }

    @Transactional
    public void delete(UUID orgId, UUID centerId) {
        OrgAccess a = access.require(orgId, AppModule.CENTER, AccessLevel.MANAGE);
        if (!a.allCenters()) {
            throw new NotFoundException("Not found.");
        }
        Center c = load(a, centerId);
        if (batches.existsByCenterId(centerId) || students.existsByCenterId(centerId)
                || memberships.anyForCenter(centerId, MembershipStatus.ACTIVE)) {
            throw new BusinessRuleException("CENTER_IN_USE", "Remove its batches, students and staff before deleting this class.");
        }
        centers.delete(c);
        audit.record(orgId, a.userId(), "CENTER", centerId, "DELETE", null);
    }

    Center load(OrgAccess a, UUID centerId) {
        a.requireCenter(centerId);
        return centers.findByIdAndOrganizationId(centerId, a.organizationId())
                .orElseThrow(() -> new NotFoundException("Class not found."));
    }

    private static void apply(Center c, CenterRequest r) {
        c.setCode(Text.clean(r.code()));
        c.setContactName(Text.clean(r.contactName()));
        c.setContactCountryCode(Text.clean(r.contactCountryCode()));
        c.setContactPhone(Text.clean(r.contactPhone()));
        c.setContactEmail(Text.clean(r.contactEmail()) == null ? null : r.contactEmail().trim().toLowerCase(java.util.Locale.ROOT));
        c.setSport(Text.clean(r.sport()));
        c.setFacilityType(Text.clean(r.facilityType()));
        c.setFacilityCount(r.facilityCount());
        c.setStatus(r.status());
        c.setCountry(Text.clean(r.country()));
        c.setState(Text.clean(r.state()));
        c.setCity(Text.clean(r.city()));
        c.setAddress1(Text.clean(r.address1()));
        c.setAddress2(Text.clean(r.address2()));
        c.setZipcode(Text.clean(r.zipcode()));
        c.setLatitude(r.latitude());
        c.setLongitude(r.longitude());
        if (r.geofenceRadiusM() != null) c.setGeofenceRadiusM(r.geofenceRadiusM());
    }

    private static CenterResponse toResponse(Center c) {
        return new CenterResponse(c.getId(), c.getName(), c.getCode(), c.getContactName(), c.getContactCountryCode(),
                c.getContactPhone(), c.getContactEmail(), c.getSport(), c.getFacilityType(), c.getFacilityCount(),
                c.getStatus(), c.getCountry(), c.getState(), c.getCity(), c.getAddress1(), c.getAddress2(),
                c.getZipcode(), c.getLatitude(), c.getLongitude(), c.getGeofenceRadiusM());
    }
}
