package com.athleet.core.iam.domain;

import com.athleet.core.common.persistence.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.Table;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "role")
public class Role extends BaseEntity {

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false)
    private RoleType roleType;

    /** System roles (e.g. Owner) cannot be deleted or have their type changed. */
    @Column(name = "system_role", nullable = false)
    private boolean systemRole;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "role_permission", joinColumns = @JoinColumn(name = "role_id"))
    @MapKeyColumn(name = "module")
    @MapKeyEnumerated(EnumType.STRING)
    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false)
    private Map<AppModule, AccessLevel> permissions = new HashMap<>();

    protected Role() {
    }

    public Role(UUID organizationId, String name, RoleType roleType, boolean systemRole) {
        this.organizationId = organizationId;
        this.name = name;
        this.roleType = roleType;
        this.systemRole = systemRole;
    }

    public AccessLevel levelFor(AppModule module) {
        return permissions.getOrDefault(module, AccessLevel.NONE);
    }

    public Map<AppModule, AccessLevel> permissionView() {
        Map<AppModule, AccessLevel> all = new EnumMap<>(AppModule.class);
        for (AppModule m : AppModule.values()) all.put(m, levelFor(m));
        return all;
    }

    public void replacePermissions(Map<AppModule, AccessLevel> next) {
        permissions.clear();
        next.forEach((m, l) -> {
            if (l != AccessLevel.NONE) permissions.put(m, l);
        });
    }

    public UUID getOrganizationId() { return organizationId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public RoleType getRoleType() { return roleType; }
    public void setRoleType(RoleType roleType) { this.roleType = roleType; }
    public boolean isSystemRole() { return systemRole; }
}
