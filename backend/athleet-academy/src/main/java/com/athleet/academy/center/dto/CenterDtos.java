package com.athleet.academy.center.dto;

import com.athleet.academy.center.domain.ActiveStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CenterDtos {

    private CenterDtos() {
    }

    public static final String TEXT = "^[\\p{L}\\p{N} .,'&()/#:-]*$";

    public record CenterRequest(
            @NotBlank @Size(max = 100) @Pattern(regexp = TEXT, message = "contains invalid characters") String name,
            @Size(max = 30) @Pattern(regexp = "^[A-Za-z0-9-]*$", message = "letters, digits and - only") String code,
            @Size(max = 100) @Pattern(regexp = TEXT, message = "contains invalid characters") String contactName,
            @Pattern(regexp = "^\\+?\\d{1,4}$", message = "invalid country code") String contactCountryCode,
            @Pattern(regexp = "^\\d{6,15}$", message = "must be 6-15 digits") String contactPhone,
            @Email @Size(max = 254) String contactEmail,
            @Size(max = 60) @Pattern(regexp = TEXT, message = "contains invalid characters") String sport,
            @Size(max = 60) @Pattern(regexp = TEXT, message = "contains invalid characters") String facilityType,
            @Min(0) @Max(1000) Integer facilityCount,
            @NotNull ActiveStatus status,
            @Size(max = 100) @Pattern(regexp = TEXT, message = "contains invalid characters") String country,
            @Size(max = 100) @Pattern(regexp = TEXT, message = "contains invalid characters") String state,
            @Size(max = 100) @Pattern(regexp = TEXT, message = "contains invalid characters") String city,
            @Size(max = 200) String address1,
            @Size(max = 200) String address2,
            @Pattern(regexp = "^[A-Za-z0-9 -]{0,12}$", message = "invalid zipcode") String zipcode,
            @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
            @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
            @Min(20) @Max(5000) Integer geofenceRadiusM) {
    }

    public record CenterSummary(UUID id, String name, ActiveStatus status, String city, String country,
                                long batches, long students, long capacity) {
    }

    public record CenterResponse(UUID id, String name, String code, String contactName, String contactCountryCode,
                                 String contactPhone, String contactEmail, String sport, String facilityType,
                                 Integer facilityCount, ActiveStatus status, String country, String state, String city,
                                 String address1, String address2, String zipcode, BigDecimal latitude,
                                 BigDecimal longitude, int geofenceRadiusM) {
    }

    public record SlotDto(@NotNull DayOfWeek dayOfWeek, @NotNull LocalTime startTime, @NotNull LocalTime endTime) {
        @JsonIgnore
        @AssertTrue(message = "end time must be after start time")
        public boolean isOrdered() {
            return startTime == null || endTime == null || endTime.isAfter(startTime);
        }
    }

    public record BatchRequest(
            @NotBlank @Size(max = 100) @Pattern(regexp = TEXT, message = "contains invalid characters") String name,
            LocalDate startDate,
            LocalDate endDate,
            @NotNull @Size(max = 7) List<@Valid SlotDto> slots,
            @Size(max = 20) Set<UUID> coachUserIds,
            @NotNull @Min(1) @Max(10000) Integer capacity,
            @NotNull ActiveStatus status,
            @Size(max = 500) String description) {

        @JsonIgnore
        @AssertTrue(message = "end date must not be before start date")
        public boolean isDateRangeValid() {
            return startDate == null || endDate == null || !endDate.isBefore(startDate);
        }
    }

    public record CoachRef(UUID userId, String name) {
    }

    public record BatchResponse(UUID id, UUID centerId, String name, LocalDate startDate, LocalDate endDate,
                                List<SlotDto> slots, List<CoachRef> coaches, int capacity, ActiveStatus status,
                                String description, long students) {
    }

    /** Lightweight option for dropdowns (Class → Batch). */
    public record Option(UUID id, String name, UUID parentId) {
    }
}
