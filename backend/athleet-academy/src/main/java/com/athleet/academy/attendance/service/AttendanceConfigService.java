package com.athleet.academy.attendance.service;

import com.athleet.academy.attendance.domain.AttendanceConfig;
import com.athleet.academy.attendance.dto.AttendanceDtos.ConfigRequest;
import com.athleet.academy.attendance.dto.AttendanceDtos.ConfigResponse;
import com.athleet.academy.attendance.repository.AttendanceConfigRepository;
import com.athleet.core.audit.service.AuditService;
import com.athleet.core.common.error.BusinessRuleException;
import com.athleet.core.iam.domain.AccessLevel;
import com.athleet.core.iam.domain.AppModule;
import com.athleet.core.iam.service.AccessService;
import com.athleet.core.iam.service.OrgAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/** Attendance Configuration (Acadware Settings): the rules the other attendance services enforce. */
@Service
public class AttendanceConfigService {

    private final AccessService access;
    private final AttendanceConfigRepository configs;
    private final AttendanceClock clock;
    private final AuditService audit;

    public AttendanceConfigService(AccessService access, AttendanceConfigRepository configs, AttendanceClock clock,
                                   AuditService audit) {
        this.access = access;
        this.configs = configs;
        this.clock = clock;
        this.audit = audit;
    }

    /** Any staff member may read the rules: the punch screen needs to know whether a selfie is required. */
    @Transactional(readOnly = true)
    public ConfigResponse get(UUID orgId) {
        access.resolve(orgId);
        return toResponse(forOrganization(orgId));
    }

    @Transactional
    public ConfigResponse update(UUID orgId, ConfigRequest req) {
        OrgAccess a = access.require(orgId, AppModule.SETTINGS, AccessLevel.MANAGE);
        AttendanceConfig c = configs.findByOrganizationId(orgId).orElseGet(() -> new AttendanceConfig(orgId));
        c.setSelfieRequired(req.selfieRequired());
        c.setTasksOnOut(req.tasksOnOut());
        c.setShowDeviceInfo(req.showDeviceInfo());
        c.setAllowDateOverride(req.allowDateOverride());
        c.setMaxBackDays(req.maxBackDays());
        c.setMaxForwardDays(req.maxForwardDays());
        configs.save(c);
        audit.record(orgId, a.userId(), "ATTENDANCE_CONFIG", c.getId(), "UPDATE",
                "selfie=" + c.isSelfieRequired() + " tasks=" + c.isTasksOnOut() + " device=" + c.isShowDeviceInfo()
                        + " override=" + c.isAllowDateOverride() + " back=" + c.getMaxBackDays() + " fwd=" + c.getMaxForwardDays());
        return toResponse(c);
    }

    /** For other attendance services (they check access themselves). Defaults when the organization never saved settings. */
    @Transactional(readOnly = true)
    public AttendanceConfig forOrganization(UUID orgId) {
        return configs.findByOrganizationId(orgId).orElseGet(() -> new AttendanceConfig(orgId));
    }

    /** Dates a coach or staff member may enter: today, or today -back/+forward days when overriding is allowed. */
    public void checkEntryDate(AttendanceConfig cfg, LocalDate date) {
        LocalDate today = clock.today();
        int back = cfg.isAllowDateOverride() ? cfg.getMaxBackDays() : 0;
        int fwd = cfg.isAllowDateOverride() ? cfg.getMaxForwardDays() : 0;
        if (date.isBefore(today.minusDays(back)) || date.isAfter(today.plusDays(fwd))) {
            throw new BusinessRuleException("DATE_NOT_ALLOWED", cfg.isAllowDateOverride()
                    ? "Attendance can only be entered from " + back + " day(s) back to " + fwd + " day(s) ahead."
                    : "Attendance can only be entered for today.");
        }
    }

    static ConfigResponse toResponse(AttendanceConfig c) {
        return new ConfigResponse(c.isSelfieRequired(), c.isTasksOnOut(), c.isShowDeviceInfo(), c.isAllowDateOverride(),
                c.getMaxBackDays(), c.getMaxForwardDays());
    }
}
