package com.athleet.academy.dashboard.service;

import com.athleet.academy.center.domain.Center;
import com.athleet.academy.center.repository.BatchRepository;
import com.athleet.academy.center.repository.CenterRepository;
import com.athleet.academy.dashboard.dto.DashboardDtos.GeneralDashboard;
import com.athleet.academy.dashboard.dto.DashboardDtos.HeadcountRow;
import com.athleet.academy.dashboard.dto.DashboardDtos.UtilizationRow;
import com.athleet.academy.student.domain.Gender;
import com.athleet.academy.student.repository.StudentRepository;
import com.athleet.core.common.error.NotFoundException;
import com.athleet.core.iam.domain.AccessLevel;
import com.athleet.core.iam.domain.AppModule;
import com.athleet.core.iam.service.AccessService;
import com.athleet.core.iam.service.OrgAccess;
import com.athleet.core.organization.domain.Organization;
import com.athleet.core.organization.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DashboardService {

    private static final String[] AGE_LABELS = {"Under 6", "6-8", "9-11", "12-14", "15-17", "18+"};

    private final AccessService access;
    private final OrganizationRepository organizations;
    private final CenterRepository centers;
    private final BatchRepository batches;
    private final StudentRepository students;

    public DashboardService(AccessService access, OrganizationRepository organizations, CenterRepository centers,
                            BatchRepository batches, StudentRepository students) {
        this.access = access;
        this.organizations = organizations;
        this.centers = centers;
        this.batches = batches;
        this.students = students;
    }

    /**
     * For a primary organization with includeAffiliates, figures cover every affiliated academy the caller can see
     * and rows are grouped per academy; otherwise rows are grouped per class.
     */
    @Transactional(readOnly = true)
    public GeneralDashboard general(UUID orgId, boolean includeAffiliates) {
        OrgAccess a = access.require(orgId, AppModule.DASHBOARD, AccessLevel.VIEW);
        Organization org = organizations.findById(orgId).orElseThrow(() -> new NotFoundException("Organization not found."));

        Map<UUID, String> orgNames = new LinkedHashMap<>();
        orgNames.put(org.getId(), org.getName());
        boolean grouped = includeAffiliates && org.isPrimary() && a.allCenters();
        if (grouped) {
            for (Organization aff : organizations.findByParentIdOrderByNameAsc(orgId)) {
                if (canView(aff.getId())) orgNames.put(aff.getId(), aff.getName());
            }
        }
        UUID centerScope = a.centerId();

        List<Center> centerList = centers.findByOrganizationIdInOrderByNameAsc(orgNames.keySet()).stream()
                .filter(c -> centerScope == null || c.getId().equals(centerScope)).toList();
        Map<UUID, UUID> centerOrg = new HashMap<>();
        centerList.forEach(c -> centerOrg.put(c.getId(), c.getOrganizationId()));

        Map<UUID, long[]> capacityByCenter = new HashMap<>();
        if (!centerList.isEmpty()) {
            for (Object[] row : batches.statsByCenter(centerOrg.keySet())) {
                capacityByCenter.put((UUID) row[0], new long[]{((Number) row[1]).longValue(), ((Number) row[2]).longValue()});
            }
        }

        // gender counts keyed by group id (academy when grouped, class otherwise)
        Map<UUID, long[]> genderByGroup = new HashMap<>();
        Map<UUID, Long> studentsByCenter = new HashMap<>();
        long male = 0, female = 0, other = 0;
        for (Object[] row : students.headcountRows(orgNames.keySet(), centerScope)) {
            UUID rowOrg = (UUID) row[0];
            UUID rowCenter = (UUID) row[1];
            Gender g = (Gender) row[2];
            long n = ((Number) row[3]).longValue();
            long[] bucket = genderByGroup.computeIfAbsent(grouped ? rowOrg : rowCenter, k -> new long[3]);
            int idx = g == Gender.MALE ? 0 : g == Gender.FEMALE ? 1 : 2;
            bucket[idx] += n;
            studentsByCenter.merge(rowCenter, n, Long::sum);
            if (idx == 0) male += n; else if (idx == 1) female += n; else other += n;
        }

        List<HeadcountRow> headcount = new ArrayList<>();
        Map<UUID, long[]> utilByGroup = new LinkedHashMap<>();
        Map<UUID, String> groupNames = new LinkedHashMap<>();
        if (grouped) {
            groupNames.putAll(orgNames);
        } else {
            centerList.forEach(c -> groupNames.put(c.getId(), c.getName()));
        }
        for (Center c : centerList) {
            UUID group = grouped ? c.getOrganizationId() : c.getId();
            long[] u = utilByGroup.computeIfAbsent(group, k -> new long[2]);
            u[0] += studentsByCenter.getOrDefault(c.getId(), 0L);
            u[1] += capacityByCenter.getOrDefault(c.getId(), new long[]{0, 0})[1];
        }
        List<UtilizationRow> utilization = new ArrayList<>();
        for (Map.Entry<UUID, String> e : groupNames.entrySet()) {
            long[] g = genderByGroup.getOrDefault(e.getKey(), new long[3]);
            headcount.add(new HeadcountRow(e.getKey(), e.getValue(), g[0], g[1], g[2], g[0] + g[1] + g[2]));
            long[] u = utilByGroup.getOrDefault(e.getKey(), new long[2]);
            utilization.add(new UtilizationRow(e.getKey(), e.getValue(), u[0], u[1], percent(u[0], u[1])));
        }
        headcount.sort(Comparator.comparingLong(HeadcountRow::total).reversed().thenComparing(HeadcountRow::name));

        long totalBatches = capacityByCenter.values().stream().mapToLong(v -> v[0]).sum();
        long totalCapacity = capacityByCenter.values().stream().mapToLong(v -> v[1]).sum();
        return new GeneralDashboard(male + female + other, centerList.size(), totalBatches, totalCapacity,
                male, female, other, ageBuckets(students.birthDates(orgNames.keySet(), centerScope)),
                grouped ? "ACADEMY" : "CENTER", headcount, utilization);
    }

    private boolean canView(UUID orgId) {
        return access.can(orgId, AppModule.DASHBOARD, AccessLevel.VIEW);
    }

    private static int percent(long part, long whole) {
        return whole <= 0 ? 0 : (int) Math.round(part * 100.0 / whole);
    }

    private static Map<String, Long> ageBuckets(List<LocalDate> dobs) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (String l : AGE_LABELS) m.put(l, 0L);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (LocalDate d : dobs) {
            int age = Period.between(d, today).getYears();
            String label = age < 6 ? AGE_LABELS[0] : age <= 8 ? AGE_LABELS[1] : age <= 11 ? AGE_LABELS[2]
                    : age <= 14 ? AGE_LABELS[3] : age <= 17 ? AGE_LABELS[4] : AGE_LABELS[5];
            m.merge(label, 1L, Long::sum);
        }
        return m;
    }
}
