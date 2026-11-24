package com.athleet.academy.attendance.service;

import java.math.BigDecimal;

/** Great-circle distance (haversine) used to flag punches made away from the center. */
final class GeoDistance {

    private static final double EARTH_RADIUS_M = 6_371_000d;

    private GeoDistance() {
    }

    static int meters(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        double p1 = Math.toRadians(lat1.doubleValue());
        double p2 = Math.toRadians(lat2.doubleValue());
        double dLat = p2 - p1;
        double dLng = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(p1) * Math.cos(p2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return (int) Math.round(EARTH_RADIUS_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }
}
