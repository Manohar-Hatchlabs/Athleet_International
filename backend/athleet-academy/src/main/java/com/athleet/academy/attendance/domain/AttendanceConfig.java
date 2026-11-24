package com.athleet.academy.attendance.domain;

import com.athleet.core.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/** Attendance Configuration (Acadware Settings). One row per organization; no row means every flag is off. */
@Entity
@Table(name = "attendance_config")
public class AttendanceConfig extends BaseEntity {

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "selfie_required", nullable = false)
    private boolean selfieRequired;

    @Column(name = "tasks_on_out", nullable = false)
    private boolean tasksOnOut;

    @Column(name = "show_device_info", nullable = false)
    private boolean showDeviceInfo;

    @Column(name = "allow_date_override", nullable = false)
    private boolean allowDateOverride;

    @Column(name = "max_back_days", nullable = false)
    private int maxBackDays;

    @Column(name = "max_forward_days", nullable = false)
    private int maxForwardDays;

    protected AttendanceConfig() {
    }

    public AttendanceConfig(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public UUID getOrganizationId() { return organizationId; }
    public boolean isSelfieRequired() { return selfieRequired; }
    public void setSelfieRequired(boolean v) { this.selfieRequired = v; }
    public boolean isTasksOnOut() { return tasksOnOut; }
    public void setTasksOnOut(boolean v) { this.tasksOnOut = v; }
    public boolean isShowDeviceInfo() { return showDeviceInfo; }
    public void setShowDeviceInfo(boolean v) { this.showDeviceInfo = v; }
    public boolean isAllowDateOverride() { return allowDateOverride; }
    public void setAllowDateOverride(boolean v) { this.allowDateOverride = v; }
    public int getMaxBackDays() { return maxBackDays; }
    public void setMaxBackDays(int v) { this.maxBackDays = v; }
    public int getMaxForwardDays() { return maxForwardDays; }
    public void setMaxForwardDays(int v) { this.maxForwardDays = v; }
}
