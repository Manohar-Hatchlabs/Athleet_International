package com.athleet.core.iam.domain;

/**
 * Admin menu sections a role can be granted (Acadware sidebar). Fees are added later.
 * <ul>
 *   <li>{@code ATTENDANCE}: view attendance; MANAGE also marks and submits student sessions.</li>
 *   <li>{@code ATTENDANCE_APPROVAL}: MANAGE approves, rejects and reopens sessions and corrects staff punches.</li>
 * </ul>
 */
public enum AppModule {
    DASHBOARD, STUDENT, CENTER, ROLE, STAFF, REPORTS, REQUEST, EXPENSE, MESSAGING, POSTS, ASSESSMENT, COACHING_DRILLS, SETTINGS,
    ATTENDANCE, ATTENDANCE_APPROVAL
}
