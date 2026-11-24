package com.athleet.core.iam.service;

import com.athleet.core.iam.domain.AccessLevel;
import com.athleet.core.iam.domain.AppModule;
import com.athleet.core.iam.domain.Role;
import com.athleet.core.iam.domain.RoleType;
import com.athleet.core.iam.repository.RoleRepository;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.athleet.core.iam.domain.AccessLevel.MANAGE;
import static com.athleet.core.iam.domain.AccessLevel.VIEW;
import static com.athleet.core.iam.domain.AppModule.*;

/** Roles every new organization starts with (mirrors Acadware's defaults). */
@Component
public class DefaultRoles {

    public static final String OWNER = "Owner";

    private final RoleRepository roles;

    public DefaultRoles(RoleRepository roles) {
        this.roles = roles;
    }

    /** Creates the default roles and returns the Owner role. */
    public Role seed(UUID orgId) {
        Role owner = role(orgId, OWNER, RoleType.HEAD_OFFICE, true, AccessService.allManage());
        Map<AppModule, AccessLevel> admin = AccessService.allManage();
        Map<AppModule, AccessLevel> manager = perms(MANAGE, DASHBOARD, STUDENT, CENTER, STAFF, REPORTS, REQUEST, EXPENSE,
                MESSAGING, POSTS, ASSESSMENT, COACHING_DRILLS, ATTENDANCE, ATTENDANCE_APPROVAL);
        Map<AppModule, AccessLevel> centerManager = perms(MANAGE, STUDENT, REQUEST, EXPENSE, MESSAGING, POSTS, ASSESSMENT,
                COACHING_DRILLS, ATTENDANCE, ATTENDANCE_APPROVAL);
        centerManager.put(DASHBOARD, VIEW);
        centerManager.put(CENTER, VIEW);
        centerManager.put(REPORTS, VIEW);
        Map<AppModule, AccessLevel> coach = perms(VIEW, DASHBOARD, STUDENT, COACHING_DRILLS);
        coach.put(ASSESSMENT, MANAGE);
        coach.put(EXPENSE, MANAGE);
        coach.put(ATTENDANCE, MANAGE);
        roles.saveAll(List.of(
                owner,
                role(orgId, "Admin", RoleType.HEAD_OFFICE, false, admin),
                role(orgId, "Manager", RoleType.HEAD_OFFICE, false, manager),
                role(orgId, "Center Manager", RoleType.CENTER, false, centerManager),
                role(orgId, "Coach", RoleType.CENTER, false, coach),
                role(orgId, "Student", RoleType.STUDENT, true, Map.of())));
        return owner;
    }

    private static Role role(UUID orgId, String name, RoleType type, boolean system, Map<AppModule, AccessLevel> p) {
        Role r = new Role(orgId, name, type, system);
        r.replacePermissions(p);
        return r;
    }

    private static Map<AppModule, AccessLevel> perms(AccessLevel level, AppModule... modules) {
        Map<AppModule, AccessLevel> m = new EnumMap<>(AppModule.class);
        for (AppModule a : modules) m.put(a, level);
        return m;
    }
}
