package com.athleet.academy.attendance.service;

import com.athleet.academy.attendance.domain.AttendanceConfig;
import com.athleet.academy.attendance.domain.PunchSource;
import com.athleet.academy.attendance.domain.StaffAttendance;
import com.athleet.academy.attendance.dto.AttendanceDtos.AdminCorrectionRequest;
import com.athleet.academy.attendance.dto.AttendanceDtos.AdminShiftRequest;
import com.athleet.academy.attendance.dto.AttendanceDtos.MyDay;
import com.athleet.academy.attendance.dto.AttendanceDtos.PunchInRequest;
import com.athleet.academy.attendance.dto.AttendanceDtos.PunchOutRequest;
import com.athleet.academy.attendance.dto.AttendanceDtos.ShiftDetail;
import com.athleet.academy.attendance.dto.AttendanceDtos.ShiftResponse;
import com.athleet.academy.attendance.repository.StaffAttendanceRepository;
import com.athleet.academy.center.domain.Batch;
import com.athleet.academy.center.domain.Center;
import com.athleet.academy.center.repository.BatchRepository;
import com.athleet.academy.center.repository.CenterRepository;
import com.athleet.academy.common.Text;
import com.athleet.core.audit.service.AuditService;
import com.athleet.core.common.error.BusinessRuleException;
import com.athleet.core.common.error.ForbiddenException;
import com.athleet.core.common.error.NotFoundException;
import com.athleet.core.common.paging.PageResponse;
import com.athleet.core.common.paging.Pages;
import com.athleet.core.iam.domain.AccessLevel;
import com.athleet.core.iam.domain.AppModule;
import com.athleet.core.iam.domain.AppUser;
import com.athleet.core.iam.domain.Membership;
import com.athleet.core.iam.domain.MembershipStatus;
import com.athleet.core.iam.repository.AppUserRepository;
import com.athleet.core.iam.repository.MembershipRepository;
import com.athleet.core.iam.service.AccessService;
import com.athleet.core.iam.service.OrgAccess;
import com.athleet.core.iam.service.StaffContext;
import com.athleet.core.storage.domain.FilePurpose;
import com.athleet.core.storage.service.FileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Staff in/out. Staff punch themselves (web now, Staff app later, same API); supervisors with
 * {@code ATTENDANCE_APPROVAL} add or correct shifts. Location is recorded and flagged, never used to refuse a punch.
 */
@Service
public class StaffAttendanceService {

    private static final Map<String, String> SORTS = Map.of("in", "inAt", "date", "workDate");
    private static final Duration FUTURE_SKEW = Duration.ofMinutes(5);
    private static final Duration MAX_SHIFT = Duration.ofHours(24);
    private static final int MAX_RANGE_DAYS = 62;
    private static final int ADMIN_MAX_BACK_DAYS = 31;

    private final AccessService access;
    private final StaffAttendanceRepository shifts;
    private final CenterRepository centers;
    private final BatchRepository batches;
    private final MembershipRepository memberships;
    private final AppUserRepository users;
    private final AttendanceConfigService config;
    private final AttendanceClock clock;
    private final FileService files;
    private final AuditService audit;

    public StaffAttendanceService(AccessService access, StaffAttendanceRepository shifts, CenterRepository centers,
                                  BatchRepository batches, MembershipRepository memberships, AppUserRepository users,
                                  AttendanceConfigService config, AttendanceClock clock, FileService files, AuditService audit) {
        this.access = access;
        this.shifts = shifts;
        this.centers = centers;
        this.batches = batches;
        this.memberships = memberships;
        this.users = users;
        this.config = config;
        this.clock = clock;
        this.files = files;
        this.audit = audit;
    }

    // ---------------------------------------------------------------- self service

    @Transactional(readOnly = true)
    public MyDay today(UUID orgId) {
        StaffContext sc = access.requireStaff(orgId);
        AttendanceConfig cfg = config.forOrganization(orgId);
        LocalDate today = clock.today();
        List<StaffAttendance> list = shifts.findByOrganizationIdAndUserIdAndWorkDateBetweenOrderByInAtAsc(orgId, sc.userId(), today, today);
        StaffAttendance open = shifts.findFirstByOrganizationIdAndUserIdAndOutAtIsNullOrderByInAtDesc(orgId, sc.userId()).orElse(null);
        Names names = names(orgId, concat(list, open));
        return new MyDay(today, AttendanceConfigService.toResponse(cfg), open == null ? null : names.shift(open, cfg),
                list.stream().map(s -> names.shift(s, cfg)).toList());
    }

    @Transactional(readOnly = true)
    public List<ShiftResponse> mine(UUID orgId, LocalDate from, LocalDate to) {
        StaffContext sc = access.requireStaff(orgId);
        checkRange(from, to);
        AttendanceConfig cfg = config.forOrganization(orgId);
        List<StaffAttendance> list = shifts.findByOrganizationIdAndUserIdAndWorkDateBetweenOrderByInAtAsc(orgId, sc.userId(), from, to);
        Names names = names(orgId, list);
        return list.stream().map(s -> names.shift(s, cfg)).toList();
    }

    @Transactional
    public ShiftResponse punchIn(UUID orgId, PunchInRequest req) {
        StaffContext sc = access.requireStaff(orgId);
        AttendanceConfig cfg = config.forOrganization(orgId);
        Optional<StaffAttendance> replay = shifts.findFirstByOrganizationIdAndUserIdAndInClientId(orgId, sc.userId(), req.clientId());
        if (replay.isPresent()) {
            return one(orgId, replay.get(), cfg);
        }
        Instant inAt = punchTime(req.capturedAt(), cfg);
        LocalDate workDate = clock.dateOf(inAt);
        if (shifts.existsByOrganizationIdAndUserIdAndWorkDateAndOutAtIsNull(orgId, sc.userId(), workDate)) {
            throw new BusinessRuleException("ALREADY_IN", "You are already punched in. Punch out first.");
        }
        UUID centerId = punchCenter(sc, orgId, req.centerId());
        Batch batch = req.batchId() == null ? null : punchBatch(orgId, centerId, req.batchId());
        if (batch != null && centerId == null) {
            centerId = batch.getCenterId();
        }
        UUID selfie = selfie(orgId, sc.userId(), req.selfieFileId(), cfg);
        Geo geo = geo(orgId, centerId, req.latitude(), req.longitude());

        StaffAttendance s = new StaffAttendance(orgId, sc.userId(), centerId, batch == null ? null : batch.getId(), workDate, inAt,
                req.capturedAt() == null ? PunchSource.WEB : PunchSource.APP);
        s.recordIn(req.clientId(), req.latitude(), req.longitude(), req.accuracyM(), geo.distanceM, geo.outside, selfie,
                Text.clean(req.deviceId()), Text.clean(req.phoneType()));
        shifts.save(s);
        audit.record(orgId, sc.userId(), "STAFF_ATTENDANCE", s.getId(), "PUNCH_IN", null);
        return one(orgId, s, cfg);
    }

    @Transactional
    public ShiftResponse punchOut(UUID orgId, PunchOutRequest req) {
        StaffContext sc = access.requireStaff(orgId);
        AttendanceConfig cfg = config.forOrganization(orgId);
        Optional<StaffAttendance> replay = shifts.findFirstByOrganizationIdAndUserIdAndOutClientId(orgId, sc.userId(), req.clientId());
        if (replay.isPresent()) {
            return one(orgId, replay.get(), cfg);
        }
        StaffAttendance s = shifts.findFirstByOrganizationIdAndUserIdAndOutAtIsNullOrderByInAtDesc(orgId, sc.userId())
                .orElseThrow(() -> new BusinessRuleException("NOT_IN", "You are not punched in."));
        Instant outAt = punchTime(req.capturedAt(), cfg);
        if (outAt.isBefore(s.getInAt())) {
            throw new BusinessRuleException("OUT_BEFORE_IN", "The out time cannot be before the in time.");
        }
        String tasks = Text.clean(req.tasksAccomplished());
        if (cfg.isTasksOnOut() && tasks == null) {
            throw new BusinessRuleException("TASKS_REQUIRED", "Describe the tasks you accomplished before punching out.");
        }
        UUID selfie = selfie(orgId, sc.userId(), req.selfieFileId(), cfg);
        Geo geo = geo(orgId, s.getCenterId(), req.latitude(), req.longitude());
        s.recordOut(outAt, req.clientId(), req.latitude(), req.longitude(), req.accuracyM(), geo.distanceM, geo.outside, selfie,
                cfg.isTasksOnOut() ? tasks : null);
        audit.record(orgId, sc.userId(), "STAFF_ATTENDANCE", s.getId(), "PUNCH_OUT", null);
        return one(orgId, s, cfg);
    }

    // ---------------------------------------------------------------- supervisors

    @Transactional(readOnly = true)
    public PageResponse<ShiftResponse> list(UUID orgId, UUID centerId, UUID userId, LocalDate from, LocalDate to,
                                            int page, int size, String sort) {
        OrgAccess a = access.require(orgId, AppModule.ATTENDANCE, AccessLevel.VIEW);
        UUID scopeCenter = centerId;
        if (!a.allCenters()) {
            if (centerId != null) a.requireCenter(centerId);
            scopeCenter = a.centerId();
        }
        LocalDate end = to == null ? clock.today() : to;
        LocalDate start = from == null ? end.minusDays(6) : from;
        checkRange(start, end);
        AttendanceConfig cfg = config.forOrganization(orgId);
        Page<StaffAttendance> result = shifts.search(orgId, scopeCenter, userId, start, end,
                Pages.of(page, size, sort, SORTS, Sort.by(Sort.Direction.DESC, "inAt").and(Sort.by("id"))));
        Names names = names(orgId, result.getContent());
        return PageResponse.of(result, s -> names.shift(s, cfg));
    }

    /** One shift with short-lived selfie links. Staff may open their own shifts; supervisors those in their scope. */
    @Transactional(readOnly = true)
    public ShiftDetail detail(UUID orgId, UUID shiftId) {
        OrgAccess a = access.resolve(orgId);
        StaffAttendance s = shifts.findByIdAndOrganizationId(shiftId, orgId)
                .orElseThrow(() -> new NotFoundException("Shift not found."));
        if (!s.getUserId().equals(a.userId())) {
            if (!a.can(AppModule.ATTENDANCE, AccessLevel.VIEW)) {
                throw new ForbiddenException("Missing permission ATTENDANCE:VIEW");
            }
            if (a.centerId() != null && !a.centerId().equals(s.getCenterId())) {
                throw new NotFoundException("Shift not found.");
            }
        }
        AttendanceConfig cfg = config.forOrganization(orgId);
        return new ShiftDetail(names(orgId, List.of(s)).shift(s, cfg),
                s.getInSelfieFileId() == null ? null : files.downloadUrl(s.getInSelfieFileId()),
                s.getOutSelfieFileId() == null ? null : files.downloadUrl(s.getOutSelfieFileId()),
                s.getCorrectionReason());
    }

    @Transactional
    public ShiftResponse addShift(UUID orgId, AdminShiftRequest req) {
        OrgAccess a = access.require(orgId, AppModule.ATTENDANCE_APPROVAL, AccessLevel.MANAGE);
        Membership target = memberships.findByUserIdAndOrganizationIdAndStatus(req.userId(), orgId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Staff member not found."));
        if (a.centerId() != null && !a.centerId().equals(target.getCenterId())) {
            throw new NotFoundException("Staff member not found.");
        }
        UUID centerId = req.centerId() != null ? req.centerId() : target.getCenterId();
        if (centerId != null) {
            a.requireCenter(centerId);
            centers.findByIdAndOrganizationId(centerId, orgId).orElseThrow(() -> new NotFoundException("Class not found."));
        }
        validateTimes(req.inAt(), req.outAt());
        LocalDate workDate = clock.dateOf(req.inAt());
        if (req.outAt() == null && shifts.existsByOrganizationIdAndUserIdAndWorkDateAndOutAtIsNull(orgId, req.userId(), workDate)) {
            throw new BusinessRuleException("ALREADY_IN", "This staff member already has an open shift that day.");
        }
        StaffAttendance s = new StaffAttendance(orgId, req.userId(), centerId, null, workDate, req.inAt(), PunchSource.ADMIN);
        s.recordOut(req.outAt(), null, null, null, null, null, null, null, null);
        s.correct(a.userId(), clock.now(), Text.clean(req.reason()), workDate, req.inAt(), req.outAt());
        shifts.save(s);
        audit.record(orgId, a.userId(), "STAFF_ATTENDANCE", s.getId(), "ADD", "in=" + req.inAt() + " out=" + req.outAt());
        return one(orgId, s, config.forOrganization(orgId));
    }

    @Transactional
    public ShiftResponse correct(UUID orgId, UUID shiftId, AdminCorrectionRequest req) {
        OrgAccess a = access.require(orgId, AppModule.ATTENDANCE_APPROVAL, AccessLevel.MANAGE);
        StaffAttendance s = shifts.findByIdAndOrganizationId(shiftId, orgId)
                .orElseThrow(() -> new NotFoundException("Shift not found."));
        if (a.centerId() != null && !a.centerId().equals(s.getCenterId())) {
            throw new NotFoundException("Shift not found.");
        }
        if (req.version() == null || !req.version().equals(s.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(StaffAttendance.class, s.getId());
        }
        validateTimes(req.inAt(), req.outAt());
        LocalDate workDate = clock.dateOf(req.inAt());
        if (req.outAt() == null && !(s.getOutAt() == null && workDate.equals(s.getWorkDate()))
                && shifts.existsByOrganizationIdAndUserIdAndWorkDateAndOutAtIsNull(orgId, s.getUserId(), workDate)) {
            throw new BusinessRuleException("ALREADY_IN", "This staff member already has an open shift that day.");
        }
        String before = "in=" + s.getInAt() + " out=" + s.getOutAt();
        s.correct(a.userId(), clock.now(), Text.clean(req.reason()), workDate, req.inAt(), req.outAt());
        shifts.flush();
        audit.record(orgId, a.userId(), "STAFF_ATTENDANCE", s.getId(), "CORRECT",
                "in=" + req.inAt() + " out=" + req.outAt() + " (was " + before + ")");
        return one(orgId, s, config.forOrganization(orgId));
    }

    // ---------------------------------------------------------------- helpers

    /** Server time for web punches; Staff app punches may carry the time they were made offline, within the allowed window. */
    private Instant punchTime(Instant captured, AttendanceConfig cfg) {
        Instant now = clock.now();
        if (captured == null) {
            return now;
        }
        if (captured.isAfter(now.plus(FUTURE_SKEW))) {
            throw new BusinessRuleException("DATE_NOT_ALLOWED", "The punch time is in the future.");
        }
        config.checkEntryDate(cfg, clock.dateOf(captured));
        return captured;
    }

    private void validateTimes(Instant in, Instant out) {
        Instant now = clock.now();
        if (in.isAfter(now.plus(FUTURE_SKEW)) || (out != null && out.isAfter(now.plus(FUTURE_SKEW)))) {
            throw new BusinessRuleException("DATE_NOT_ALLOWED", "Times cannot be in the future.");
        }
        if (clock.dateOf(in).isBefore(clock.today().minusDays(ADMIN_MAX_BACK_DAYS))) {
            throw new BusinessRuleException("DATE_NOT_ALLOWED", "Shifts can be corrected for the last " + ADMIN_MAX_BACK_DAYS + " days only.");
        }
        if (out != null && (out.isBefore(in) || Duration.between(in, out).compareTo(MAX_SHIFT) > 0)) {
            throw new BusinessRuleException("INVALID_TIMES", "The out time must be after the in time, within 24 hours.");
        }
    }

    /** CENTER-role staff always punch at their own center; head-office staff may name one. */
    private UUID punchCenter(StaffContext sc, UUID orgId, UUID requested) {
        if (sc.centerRole()) {
            if (requested != null && !requested.equals(sc.centerId())) {
                throw new BusinessRuleException("CENTER_MISMATCH", "You can only punch in at your own class.");
            }
            return sc.centerId();
        }
        if (requested != null && !centers.existsByIdAndOrganizationId(requested, orgId)) {
            throw new NotFoundException("Class not found.");
        }
        return requested;
    }

    private Batch punchBatch(UUID orgId, UUID centerId, UUID batchId) {
        Batch b = batches.findByIdAndOrganizationId(batchId, orgId).orElseThrow(() -> new NotFoundException("Batch not found."));
        if (centerId != null && !centerId.equals(b.getCenterId())) {
            throw new BusinessRuleException("INVALID_BATCH", "That batch does not belong to this class.");
        }
        return b;
    }

    private UUID selfie(UUID orgId, UUID userId, UUID fileId, AttendanceConfig cfg) {
        if (fileId == null) {
            if (cfg.isSelfieRequired()) {
                throw new BusinessRuleException("SELFIE_REQUIRED", "A selfie is required to punch.");
            }
            return null;
        }
        return files.attach(orgId, fileId, userId, FilePurpose.ATTENDANCE_SELFIE).getId();
    }

    private record Geo(Integer distanceM, Boolean outside) {
    }

    /** Distance to the center and whether it is beyond the geofence. Unknown (nulls) without coordinates on either side. */
    private Geo geo(UUID orgId, UUID centerId, BigDecimal lat, BigDecimal lng) {
        if (centerId == null || lat == null || lng == null) {
            return new Geo(null, null);
        }
        Center c = centers.findByIdAndOrganizationId(centerId, orgId).orElse(null);
        if (c == null || c.getLatitude() == null || c.getLongitude() == null) {
            return new Geo(null, null);
        }
        int meters = GeoDistance.meters(lat, lng, c.getLatitude(), c.getLongitude());
        return new Geo(meters, meters > c.getGeofenceRadiusM());
    }

    private ShiftResponse one(UUID orgId, StaffAttendance s, AttendanceConfig cfg) {
        return names(orgId, List.of(s)).shift(s, cfg);
    }

    private static List<StaffAttendance> concat(List<StaffAttendance> list, StaffAttendance extra) {
        if (extra == null || list.contains(extra)) {
            return list;
        }
        List<StaffAttendance> all = new java.util.ArrayList<>(list);
        all.add(extra);
        return all;
    }

    private static void checkRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from) || from.plusDays(MAX_RANGE_DAYS).isBefore(to)) {
            throw new BusinessRuleException("INVALID_RANGE", "Choose a date range of at most " + MAX_RANGE_DAYS + " days.");
        }
    }

    private Names names(UUID orgId, Collection<StaffAttendance> list) {
        Set<UUID> userIds = new HashSet<>();
        Set<UUID> centerIds = new HashSet<>();
        Set<UUID> batchIds = new HashSet<>();
        for (StaffAttendance s : list) {
            userIds.add(s.getUserId());
            if (s.getCenterId() != null) centerIds.add(s.getCenterId());
            if (s.getBatchId() != null) batchIds.add(s.getBatchId());
        }
        return new Names(
                userIds.isEmpty() ? Map.of() : users.findByIdIn(userIds).stream().collect(Collectors.toMap(AppUser::getId, AppUser::fullName)),
                centerIds.isEmpty() ? Map.of() : centers.findByIdIn(centerIds).stream()
                        .filter(c -> c.getOrganizationId().equals(orgId)).collect(Collectors.toMap(Center::getId, Center::getName)),
                batchIds.isEmpty() ? Map.of() : batches.findByIdIn(batchIds).stream()
                        .filter(b -> b.getOrganizationId().equals(orgId)).collect(Collectors.toMap(Batch::getId, Batch::getName)));
    }

    private record Names(Map<UUID, String> users, Map<UUID, String> centers, Map<UUID, String> batches) {

        /** Immutable maps (Map.of) reject null keys, and a shift may have no class or batch. */
        private static String name(Map<UUID, String> names, UUID id) {
            return id == null ? null : names.get(id);
        }

        ShiftResponse shift(StaffAttendance s, AttendanceConfig cfg) {
            return new ShiftResponse(s.getId(), s.getVersion(), s.getUserId(), name(users, s.getUserId()), s.getCenterId(),
                    name(centers, s.getCenterId()), s.getBatchId(), name(batches, s.getBatchId()), s.getWorkDate(), s.getInAt(),
                    s.getOutAt(), s.getSource(), s.getInDistanceM(), s.getInOutsideGeofence(), s.getOutDistanceM(),
                    s.getOutOutsideGeofence(), s.getInSelfieFileId() != null, s.getOutSelfieFileId() != null,
                    s.getTasksAccomplished(), cfg.isShowDeviceInfo() ? s.getDeviceId() : null,
                    cfg.isShowDeviceInfo() ? s.getPhoneType() : null, s.getCorrectedBy() != null);
        }
    }
}
