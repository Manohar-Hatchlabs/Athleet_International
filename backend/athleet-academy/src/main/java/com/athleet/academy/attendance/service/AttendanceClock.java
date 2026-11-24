package com.athleet.academy.attendance.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * "Today" for attendance is the academy's calendar day. Asia/Kolkata for now; organizations in other time zones
 * will need a per-organization setting later.
 */
@Component
public class AttendanceClock {

    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final Clock clock = Clock.system(ZONE);

    public Instant now() {
        return clock.instant();
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public LocalDate dateOf(Instant instant) {
        return instant.atZone(ZONE).toLocalDate();
    }
}
