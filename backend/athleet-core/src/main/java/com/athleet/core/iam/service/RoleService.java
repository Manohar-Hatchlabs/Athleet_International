package com.athleet.core.iam.service;

import com.athleet.core.audit.service.AuditService;
import com.athleet.core.common.error.BusinessRuleException;
import com.athleet.core.common.error.ConflictException;
import com.athleet.core.common.error.NotFoundException;
import com.athleet.core.common.paging.PageResponse;
import com.athleet.core.common.paging.Pages;
import com.athleet.core.iam.domain.AccessLevel;
import com.athleet.core.iam.domain.AppModule;
import com.athleet.core.iam.domain.Role;
import com.athleet.core.iam.domain.RoleType;
import com.athleet.core.iam.dto.RoleDtos.RoleRequest;
import com.athleet.core.iam.dto.RoleDtos.RoleResponse;
import com.athleet.core.iam.dto.RoleDtos.RoleSummary;
import com.athleet.core.iam.repository.MembershipRepository;
import com.athleet.core.iam.repository.RoleRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class RoleService {

    private static final Map<String, String> SORTS = Map.of("name", "name", "roleType", "roleType");

    private final AccessService access;
    private final RoleRepository roles;
    private final MembershipRepository memberships;
    private final AuditService audit;

    public RoleService(AccessService access, RoleRepository roles, MembershipRepository memberships, AuditService audit) {
        this.access = access;
        this.roles = roles;
        this.memberships = memberships;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageResponse<RoleSummary> list(UUID orgId, String search, int page, int size, String sort) {
        access.require(orgId, AppModule.ROLE, AccessLevel.VIEW);
        String like = search == null || search.isBlank() ? null : "%" + escapeLike(search.trim().toLowerCase(Locale.ROOT)) + "%";
        return PageResponse.of(roles.search(orgId, like, Pages.of(page, size, sort, SORTS, Sort.by("name"))), RoleService::summary);
    }

    /** Roles a staff member can be given (used by the Add Staff form, which needs STAFF access, not ROLE). */
    @Transactional(readOnly = true)
    public PageResponse<RoleSummary> assignable(UUID orgId) {
        access.require(orgId, AppModule.STAFF, AccessLevel.VIEW);
        return PageResponse.of(roles.search(orgId, null, Pages.of(0, Pages.MAX_SIZE, null, SORTS, Sort.by("name"))), RoleService::summary);
    }

    @Transactional(readOnly = true)
    public RoleResponse get(UUID orgId, UUID roleId) {
        access.require(orgId, AppModule.ROLE, AccessLevel.VIEW);
        return response(load(orgId, roleId));
    }

    @Transactional
    public RoleResponse create(UUID orgId, RoleRequest req) {
        OrgAccess a = access.require(orgId, AppModule.ROLE, AccessLevel.MANAGE);
        String name = req.name().trim();
        if (roles.existsByOrganizationIdAndNameIgnoreCase(orgId, name)) {
            throw new ConflictException("A role with this name already exists.");
        }
        Role role = new Role(orgId, name, req.roleType(), false);
        role.replacePermissions(checkedPermissions(a, req));
        roles.save(role);
        audit.record(orgId, a.userId(), "ROLE", role.getId(), "CREATE", "type=" + role.getRoleType() + " perms=" + role.permissionView());
        return response(role);
    }

    @Transactional
    public RoleResponse update(UUID orgId, UUID roleId, RoleRequest req) {
        OrgAccess a = access.require(orgId, AppModule.ROLE, AccessLevel.MANAGE);
        Role role = load(orgId, roleId);
        String name = req.name().trim();
        if (roles.existsByOrganizationIdAndNameIgnoreCaseAndIdNot(orgId, name, roleId)) {
            throw new ConflictException("A role with this name already exists.");
        }
        if (role.isSystemRole()) {
            if (req.roleType() != role.getRoleType()) {
                throw new BusinessRuleException("SYSTEM_ROLE", "The type of a system role cannot be changed.");
            }
            // System role names and permissions are fixed.
        } else {
            if (req.roleType() != role.getRoleType() && memberships.existsByRoleId(roleId)) {
                throw new BusinessRuleException("ROLE_IN_USE", "Change the type only when no staff member has this role.");
            }
            role.setName(name);
            role.setRoleType(req.roleType());
            role.replacePermissions(checkedPermissions(a, req));
        }
        audit.record(orgId, a.userId(), "ROLE", role.getId(), "UPDATE", "type=" + role.getRoleType() + " perms=" + role.permissionView());
        return response(role);
    }

    @Transactional
    public void delete(UUID orgId, UUID roleId) {
        OrgAccess a = access.require(orgId, AppModule.ROLE, AccessLevel.MANAGE);
        Role role = load(orgId, roleId);
        if (role.isSystemRole()) {
            throw new BusinessRuleException("SYSTEM_ROLE", "System roles cannot be deleted.");
        }
        if (memberships.existsByRoleId(roleId)) {
            throw new BusinessRuleException("ROLE_IN_USE", "This role is assigned to staff members.");
        }
        roles.delete(role);
        audit.record(orgId, a.userId(), "ROLE", roleId, "DELETE", role.getName());
    }

    /** Prevents privilege escalation: nobody can grant more than they hold themselves. Student roles get nothing. */
    private static Map<AppModule, AccessLevel> checkedPermissions(OrgAccess a, RoleRequest req) {
        Map<AppModule, AccessLevel> result = new EnumMap<>(AppModule.class);
        if (req.roleType() == RoleType.STUDENT || req.permissions() == null) {
            return result;
        }
        req.permissions().forEach((module, level) -> {
            if (module == null || level == null) return;
            if (!a.can(module, level)) {
                throw new BusinessRuleException("PERMISSION_ESCALATION", "You cannot grant " + module + " access above your own.");
            }
            result.put(module, level);
        });
        return result;
    }

    private Role load(UUID orgId, UUID roleId) {
        return roles.findWithPermissionsByIdAndOrganizationId(roleId, orgId)
                .orElseThrow(() -> new NotFoundException("Role not found."));
    }

    private static RoleSummary summary(Role r) {
        return new RoleSummary(r.getId(), r.getName(), r.getRoleType(), r.isSystemRole());
    }

    private static RoleResponse response(Role r) {
        return new RoleResponse(r.getId(), r.getName(), r.getRoleType(), r.isSystemRole(), r.permissionView());
    }

    static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
