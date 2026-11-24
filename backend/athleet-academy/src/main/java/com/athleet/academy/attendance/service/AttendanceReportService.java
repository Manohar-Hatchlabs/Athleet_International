package com.athleet.academy.attendance.service;

import com.athleet.academy.attendance.domain.AttendanceSession;
import com.athleet.academy.attendance.domain.MarkStatus;
import com.athleet.academy.attendance.domain.SessionStatus;
import com.athleet.academy.attendance.domain.StaffAttendance;
import com.athleet.academy.attendance.dto.AttendanceDtos.LiveAttendance;
import com.athleet.academy.attendance.dto.AttendanceDtos.LiveGroup;
import com.athleet.academy.attendance.dto.AttendanceDtos.LiveSessionRow;
import com.athleet.academy.attendance.dto.AttendanceDtos.LiveStaffRow;
import com.athleet.academy.attendance.dto.AttendanceDtos.PersonStatus;
import com.athleet.academy.attendance.dto.AttendanceDtos.StudentDay;
import com.athleet.academy.attendance.dto.AttendanceDtos.StudentMonth;
import com.athleet.academy.attendance.dto.AttendanceDtos.TrackerCell;
import com.athleet.academy.attendance.dto.AttendanceDtos.TrackerGrid;
import com.athleet.academy.attendance.dto.AttendanceDtos.TrackerRow;
import com.athleet.academy.attendance.dto.AttendanceDtos.TrackerType;
import com.athleet.academy.attendance.repository.AttendanceMarkRepository;
import com.athleet.academy.attendance.repository.AttendanceSessionRepository;
import com.athleet.academy.attendance.repository.StaffAttendanceRepository;
import com.athleet.academy.center.domain.Batch;
import com.athleet.academy.center.domain.Center;
import com.athleet.academy.center.repository.BatchRepository;
import com.athleet.academy.center.repository.CenterRepository;
import com.athleet.academy.common.Text;
import com.athleet.academy.student.domain.Student;
import com.athleet.academy.student.domain.StudentStatus;
import com.athleet.academy.student.repository.StudentRepository;
import com.athleet.core.common.error.BusinessRuleException;
import com.athleet.core.common.error.NotFoundException;
import com.athleet.core.common.paging.PageResponse;
import com.athleet.core.common.paging.Pages;
import com.athleet.core.iam.domain.AccessLevel;
import com.athleet.core.iam.domain.AppModule;
import com.athleet.core.iam.domain.AppUser;
import com.athleet.core.iam.domain.MembershipStatus;
import com.athleet.core.iam.dto.StaffDtos.StaffResponse;
import com.athleet.core.iam.repository.AppUserRepository;
import com.athleet.core.iam.repository.StaffQueryRepository;
import com.athleet.core.iam.service.AccessService;
import com.athleet.core.iam.service.OrgAccess;
import com.athleet.core.organization.domain.Organization;
import com.athleet.core.organization.repository.OrganizationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Read-only attendance views: live today, the monthly tracker grid, and one student's month. All SQL-side aggregation. */
@Service
public class AttendanceReportService {

    private static final int MAX_TRACKER_DAYS = 31;
    private static final int MAX_TRACKER_PAGE = 50;

    private final AccessService access;
    private final OrganizationRepository organizations;
    private final CenterRepository centers;
    private final BatchRepository batches;
    private final AttendanceSessionRepository sessions;
    private final AttendanceMarkRepository marks;
    private final StaffAttendanceRepository shifts;
    private final AppUserRepository users;
    private final StudentRepository students;
    private final StaffQueryRepository staffQuery;
    private final AttendanceConfigService config;
    private final AttendanceClock clock;

    public AttendanceReportService(AccessService access, OrganizationRepository organizations, CenterRepository centers,
                                   BatchRepository batches, AttendanceSessionRepository sessions, AttendanceMarkRepository marks,
                                   StaffAttendanceRepository shifts, AppUserRepository users, StudentRepository students,
                                   StaffQueryRepository staffQuery, AttendanceConfigService config, AttendanceClock clock) {
        this.access = access;
        this.organizations = organizations;
        this.centers = centers;
        this.batches = batches;
        this.sessions = sessions;
        this.marks = marks;
        this.shifts = shifts;
        this.users = users;
        this.students = students;
        this.staffQuery = staffQuery;
        this.config = config;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- live

    /**
     * Today's staff in/out and batch sessions. For a primary organization with {@code includeAffiliates} the groups are
     * the affiliated academies the caller can see; otherwise one group per center.
     */
    @Transactional(readOnly = true)
    public LiveAttendance live(UUID orgId, boolean includeAffiliates) {
        OrgAccess a = access.require(orgId, AppModule.ATTENDANCE, AccessLevel.VIEW);
        Organization org = organizations.findById(orgId).orElseThrow(() -> new NotFoundException("Organization not found."));
        Map<UUID, String> orgNames = new LinkedHashMap<>();
        orgNames.put(org.getId(), org.getName());
        boolean grouped = includeAffiliates && org.isPrimary() && a.allCenters();
        if (grouped) {
            for (Organization aff : organizations.findByParentIdOrderByNameAsc(orgId)) {
                if (access.can(aff.getId(), AppModule.ATTENDANCE, AccessLevel.VIEW)) orgNames.put(aff.getId(), aff.getName());
            }
        }
        UUID centerScope = a.centerId();
        List<Center> centerList = centers.findByOrganizationIdInOrderByNameAsc(orgNames.keySet()).stream()
                .filter(c -> centerScope == null || c.getId().equals(centerScope)).toList();
        Map<UUID, Center> centerById = centerList.stream().collect(Collectors.toMap(Center::getId, c -> c));
        LocalDate today = clock.today();

        List<Batch> todays = centerList.isEmpty() ? List.of()
                : batches.scheduledOn(centerById.keySet(), today.getDayOfWeek(), today);
        Map<UUID, AttendanceSession> sessionByBatch = todays.isEmpty() ? Map.of()
                : sessions.findByBatchIdInAndSessionDate(todays.stream().map(Batch::getId).toList(), today).stream()
                .collect(Collectors.toMap(AttendanceSession::getBatchId, s -> s));
        Map<UUID, long[]> counts = counts(sessionByBatch.values().stream().map(AttendanceSession::getId).toList());
        List<StaffAttendance> todayShifts = shifts.findByOrganizationIdInAndWorkDateOrderByInAtAsc(orgNames.keySet(), today).stream()
                .filter(s -> centerScope == null || centerScope.equals(s.getCenterId())).toList();

        Set<UUID> userIds = new HashSet<>();
        todays.forEach(b -> userIds.addAll(b.getCoachUserIds()));
        todayShifts.forEach(s -> userIds.add(s.getUserId()));
        Map<UUID, String> userNames = userIds.isEmpty() ? Map.of() : users.findByIdIn(userIds).stream()
                .collect(Collectors.toMap(AppUser::getId, AppUser::fullName));
        Set<UUID> shiftBatchIds = todayShifts.stream().map(StaffAttendance::getBatchId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, String> shiftBatchNames = shiftBatchIds.isEmpty() ? Map.of() : batches.findByIdIn(shiftBatchIds).stream()
                .filter(b -> orgNames.containsKey(b.getOrganizationId())).collect(Collectors.toMap(Batch::getId, Batch::getName));
        Map<UUID, Boolean> showDevice = new HashMap<>();
        orgNames.keySet().forEach(id -> showDevice.put(id, config.forOrganization(id).isShowDeviceInfo()));

        // group key: academy when grouped, center otherwise (centerless shifts fall under the organization itself)
        Map<UUID, String> groupNames = new LinkedHashMap<>();
        if (grouped) {
            groupNames.putAll(orgNames);
        } else {
            centerList.forEach(c -> groupNames.put(c.getId(), c.getName()));
            if (centerScope == null && todayShifts.stream().anyMatch(s -> s.getCenterId() == null)) {
                groupNames.put(orgId, "Head office");
            }
        }
        List<LiveGroup> groups = new ArrayList<>();
        for (Map.Entry<UUID, String> g : groupNames.entrySet()) {
            UUID key = g.getKey();
            List<Batch> groupBatches = todays.stream().filter(b -> keyOf(grouped, centerById.get(b.getCenterId()), orgId, null).equals(key)).toList();
            List<StaffAttendance> groupShifts = todayShifts.stream()
                    .filter(s -> keyOf(grouped, s.getCenterId() == null ? null : centerById.get(s.getCenterId()), orgId, s).equals(key)).toList();

            List<LiveSessionRow> sessionRows = groupBatches.stream().map(b -> {
                AttendanceSession s = sessionByBatch.get(b.getId());
                long[] c = s == null ? new long[3] : counts.getOrDefault(s.getId(), new long[3]);
                List<String> coaches = b.getCoachUserIds().stream().map(id -> userNames.getOrDefault(id, "")).sorted().toList();
                return new LiveSessionRow(b.getId(), b.getName(), coaches, s == null ? null : s.getStatus(), c[0], c[1], c[0] + c[1] + c[2]);
            }).toList();

            Map<UUID, List<StaffAttendance>> shiftsByUser = groupShifts.stream()
                    .collect(Collectors.groupingBy(StaffAttendance::getUserId, LinkedHashMap::new, Collectors.toList()));
            Map<UUID, List<String>> coachedToday = new HashMap<>();
            for (Batch b : groupBatches) {
                for (UUID coach : b.getCoachUserIds()) coachedToday.computeIfAbsent(coach, k -> new ArrayList<>()).add(b.getName());
            }
            Set<UUID> staffIds = new HashSet<>(shiftsByUser.keySet());
            staffIds.addAll(coachedToday.keySet());
            List<LiveStaffRow> staffRows = staffIds.stream().map(uid -> {
                List<StaffAttendance> mine = shiftsByUser.getOrDefault(uid, List.of());
                StaffAttendance first = mine.isEmpty() ? null : mine.get(0);
                StaffAttendance last = mine.isEmpty() ? null : mine.get(mine.size() - 1);
                String status = last == null ? "NOT_IN" : last.getOutAt() == null ? "IN" : "OUT";
                String batch = last != null && last.getBatchId() != null ? shiftBatchNames.get(last.getBatchId())
                        : String.join(", ", coachedToday.getOrDefault(uid, List.of()));
                boolean device = last != null && showDevice.getOrDefault(last.getOrganizationId(), false);
                Boolean outside = mine.stream().anyMatch(s -> Boolean.TRUE.equals(s.getInOutsideGeofence())) ? Boolean.TRUE
                        : mine.isEmpty() ? null : Boolean.FALSE;
                return new LiveStaffRow(uid, userNames.getOrDefault(uid, ""), status, first == null ? null : first.getInAt(),
                        last == null ? null : last.getOutAt(), batch == null || batch.isBlank() ? null : batch, outside,
                        device ? first.getDeviceId() : null, device ? first.getPhoneType() : null);
            }).sorted(Comparator.comparing(LiveStaffRow::name, String.CASE_INSENSITIVE_ORDER)).toList();
            groups.add(new LiveGroup(key, g.getValue(), staffRows, sessionRows));
        }
        return new LiveAttendance(today, grouped ? "ACADEMY" : "CENTER", groups);
    }

    private static UUID keyOf(boolean grouped, Center center, UUID orgId, StaffAttendance shift) {
        if (grouped) {
            return center != null ? center.getOrganizationId() : shift != null ? shift.getOrganizationId() : orgId;
        }
        return center != null ? center.getId() : orgId;
    }

    // ---------------------------------------------------------------- tracker

    @Transactional(readOnly = true)
    public TrackerGrid tracker(UUID orgId, TrackerType type, LocalDate from, LocalDate to, UUID centerId, UUID batchId,
                               PersonStatus status, String search, int page, int size) {
        OrgAccess a = access.require(orgId, AppModule.ATTENDANCE, AccessLevel.VIEW);
        if (to.isBefore(from) || from.plusDays(MAX_TRACKER_DAYS - 1L).isBefore(to)) {
            throw new BusinessRuleException("INVALID_RANGE", "Choose a range of at most " + MAX_TRACKER_DAYS + " days.");
        }
        UUID scopeCenter = centerId;
        if (!a.allCenters()) {
            if (centerId != null) a.requireCenter(centerId);
            scopeCenter = a.centerId();
        }
        int pageSize = Math.min(Math.max(size, 1), MAX_TRACKER_PAGE);
        List<LocalDate> days = from.datesUntil(to.plusDays(1)).toList();
        PersonStatus st = status == null ? PersonStatus.ACTIVE : status;
        return type == TrackerType.STUDENT
                ? studentGrid(orgId, from, to, days, scopeCenter, batchId, st, search, page, pageSize)
                : staffGrid(orgId, from, to, days, scopeCenter, st, search, page, pageSize);
    }

    private TrackerGrid studentGrid(UUID orgId, LocalDate from, LocalDate to, List<LocalDate> days, UUID centerId, UUID batchId,
                                    PersonStatus status, String search, int page, int size) {
        StudentStatus ss = status == PersonStatus.ACTIVE ? StudentStatus.ACTIVE : StudentStatus.DEREGISTERED;
        Page<Student> result = students.search(orgId, ss, centerId, batchId, Text.likePattern(search),
                Pages.of(page, size, null, Map.of(), Sort.by("firstName").and(Sort.by("id"))));
        List<UUID> ids = result.getContent().stream().map(Student::getId).toList();
        Map<UUID, Map<LocalDate, boolean[]>> perStudent = new HashMap<>(); // [present, pending]
        if (!ids.isEmpty()) {
            for (Object[] row : marks.forStudents(orgId, ids, from, to)) {
                MarkStatus ms = (MarkStatus) row[2];
                if (ms == null) continue;
                boolean[] cell = perStudent.computeIfAbsent((UUID) row[0], k -> new HashMap<>())
                        .computeIfAbsent((LocalDate) row[1], k -> new boolean[]{false, false});
                cell[0] |= ms == MarkStatus.PRESENT;
                cell[1] |= row[3] != SessionStatus.APPROVED;
            }
        }
        return new TrackerGrid("STUDENT", from, to, days, result.getContent().stream().map(s -> {
            Map<LocalDate, boolean[]> mine = perStudent.getOrDefault(s.getId(), Map.of());
            List<TrackerCell> cells = new ArrayList<>();
            long present = 0, absent = 0;
            for (LocalDate d : days) {
                boolean[] c = mine.get(d);
                if (c == null) continue;
                cells.add(new TrackerCell(d, c[0] ? "P" : "A", c[1], null, null, null));
                if (c[0]) present++; else absent++;
            }
            String name = s.getLastName() == null ? s.getFirstName() : s.getFirstName() + " " + s.getLastName();
            return new TrackerRow(s.getId(), name, "#" + s.getStudentNo(), cells, present, absent, percent(present, present + absent));
        }).toList(), result.getNumber(), result.getSize(), result.getTotalElements());
    }

    private TrackerGrid staffGrid(UUID orgId, LocalDate from, LocalDate to, List<LocalDate> days, UUID centerId,
                                  PersonStatus status, String search, int page, int size) {
        MembershipStatus ms = status == PersonStatus.ACTIVE ? MembershipStatus.ACTIVE : MembershipStatus.DEREGISTERED;
        PageResponse<StaffResponse> people = staffQuery.search(orgId, search, ms, centerId, page, size);
        List<UUID> userIds = people.items().stream().map(StaffResponse::userId).toList();
        Map<UUID, Map<LocalDate, List<StaffAttendance>>> perUser = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (StaffAttendance s : shifts.findByOrganizationIdAndUserIdInAndWorkDateBetweenOrderByInAtAsc(orgId, userIds, from, to)) {
                perUser.computeIfAbsent(s.getUserId(), k -> new HashMap<>()).computeIfAbsent(s.getWorkDate(), k -> new ArrayList<>()).add(s);
            }
        }
        List<TrackerRow> rows = people.items().stream().map(p -> {
            Map<LocalDate, List<StaffAttendance>> mine = perUser.getOrDefault(p.userId(), Map.of());
            List<TrackerCell> cells = new ArrayList<>();
            for (LocalDate d : days) {
                List<StaffAttendance> day = mine.get(d);
                if (day == null || day.isEmpty()) continue;
                StaffAttendance first = day.get(0);
                StaffAttendance last = day.get(day.size() - 1);
                boolean outside = day.stream().anyMatch(s -> Boolean.TRUE.equals(s.getInOutsideGeofence()));
                cells.add(new TrackerCell(d, last.getOutAt() == null ? "OPEN" : "P", false, first.getInAt(), last.getOutAt(), outside));
            }
            String name = p.lastName() == null ? p.firstName() : p.firstName() + " " + p.lastName();
            return new TrackerRow(p.userId(), name, p.roleName(), cells, cells.size(), 0, 0);
        }).toList();
        return new TrackerGrid("STAFF", from, to, days, rows, people.page(), people.size(), people.total());
    }

    // ---------------------------------------------------------------- one student

    @Transactional(readOnly = true)
    public StudentMonth studentMonth(UUID orgId, UUID studentId, String month) {
        OrgAccess a = access.require(orgId, AppModule.ATTENDANCE, AccessLevel.VIEW);
        Student s = students.findByIdAndOrganizationId(studentId, orgId).orElseThrow(() -> new NotFoundException("Student not found."));
        a.requireCenter(s.getCenterId());
        YearMonth ym;
        try {
            ym = month == null || month.isBlank() ? YearMonth.from(clock.today()) : YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new BusinessRuleException("INVALID_MONTH", "Use the format YYYY-MM.");
        }
        List<Object[]> rows = marks.forStudents(orgId, List.of(studentId), ym.atDay(1), ym.atEndOfMonth());
        Map<UUID, String> batchNames = batches.findByIdIn(rows.stream().map(r -> (UUID) r[4]).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Batch::getId, Batch::getName));
        List<StudentDay> days = rows.stream().filter(r -> r[2] != null)
                .map(r -> new StudentDay((LocalDate) r[1], batchNames.get((UUID) r[4]), (MarkStatus) r[2], (SessionStatus) r[3]))
                .sorted(Comparator.comparing(StudentDay::date).thenComparing(d -> d.batchName() == null ? "" : d.batchName())).toList();
        long present = days.stream().filter(d -> d.status() == MarkStatus.PRESENT).count();
        long absent = days.size() - present;
        String name = s.getLastName() == null ? s.getFirstName() : s.getFirstName() + " " + s.getLastName();
        return new StudentMonth(s.getId(), name, ym.toString(), present, absent, percent(present, present + absent), days);
    }

    // ---------------------------------------------------------------- helpers

    private Map<UUID, long[]> counts(List<UUID> sessionIds) {
        Map<UUID, long[]> out = new HashMap<>();
        if (sessionIds.isEmpty()) {
            return out;
        }
        for (Object[] row : marks.countsBySession(sessionIds)) {
            long[] c = out.computeIfAbsent((UUID) row[0], k -> new long[3]);
            MarkStatus st = (MarkStatus) row[1];
            c[st == MarkStatus.PRESENT ? 0 : st == MarkStatus.ABSENT ? 1 : 2] += ((Number) row[2]).longValue();
        }
        return out;
    }

    private static int percent(long part, long whole) {
        return whole <= 0 ? 0 : (int) Math.round(part * 100.0 / whole);
    }
}
