package com.athleet.academy.dashboard.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    /** Headcount for one academy or class, split by gender. */
    public record HeadcountRow(UUID id, String name, long male, long female, long other, long total) {
    }

    /** Students against batch capacity. utilization is a percentage (0 when there is no capacity). */
    public record UtilizationRow(UUID id, String name, long students, long capacity, int utilization) {
    }

    /** Acadware Dashboard → General tab (Headcount + Utilization). */
    public record GeneralDashboard(long totalStudents, long totalCenters, long totalBatches, long totalCapacity,
                                   long male, long female, long other,
                                   Map<String, Long> ageBuckets,
                                   String groupedBy,
                                   List<HeadcountRow> headcount,
                                   List<UtilizationRow> utilization) {
    }
}
