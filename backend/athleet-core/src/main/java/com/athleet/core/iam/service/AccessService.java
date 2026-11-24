package com.athleet.core.iam.service;

import com.athleet.core.common.error.BusinessRuleException;
import com.athleet.core.common.error.ForbiddenException;
import com.athleet.core.common.error.NotFoundException;
import com.athleet.core.common.security.AuthUser;
import com.athleet.core.common.security.CurrentUser;
import com.athleet.core.iam.domain.AccessLevel;
import com.athleet.core.iam.domain.AppModule;
import com.athleet.core.iam.domain.Membership;
import com.athleet.core.iam.domain.MembershipStatus;
import com.athleet.core.iam.domain.Role;
import com.athleet.core.iam.domain.RoleType;
import com.athleet.core.iam.repository.MembershipRepository;
import com.athleet.core.iam.repository.RoleRepository;
import com.athleet.core.organization.domain.Organization;
import com.athleet.core.organization.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Central authorization: every organization-scoped service call goes through {@link #require}.
 * <ul>
 *   <li>Platform admins can manage everything.</li>
 *   <li>A membership in the organization grants its role's permissions.</li>
 *   <li>A HEAD_OFFICE membership in a primary organization also applies to its affiliated academies.</li>
 *   <li>Anything else is reported as 404, so organization ids cannot be probed.</li>
 * </ul>
 */
@Service
public class AccessService {

    private final CurrentUser currentUser;
    private final OrganizationRepository organizations;
    private final MembershipRepository memberships;
    private final RoleRepository roles;

    public AccessService(CurrentUser currentUser, OrganizationRepository organizations,
                         MembershipRepository memberships, RoleRepository roles) {
        this.currentUser = currentUser;
        this.organizations = organizations;
        this.memberships = memberships;
        this.roles = roles;
    }

    @Transactional(readOnly = true)
    public OrgAccess require(UUID organizationId, AppModule module, AccessLevel level) {
        OrgAccess access = resolve(organizationId);
        if (!access.can(module, level)) {
            throw new ForbiddenException("Missing permission " + module + ":" + level);
        }
        return access;
    }

    @Transactional(readOnly = true)
    public OrgAccess resolve(UUID organizationId) {
        return find(organizationId).orElseThrow(() -> new NotFoundException("Organization not found."));
    }

    /**
     * The caller's own active, non-student staff membership in this organization. Used for self-service actions
     * (punch in/out, uploads) that belong to a person rather than to a permission.
     */
    @Transactional(readOnly = true)
    public StaffContext requireStaff(UUID organizationId) {
        AuthUser user = currentUser.require();
        Membership m = memberships.findByUserIdAndOrganizationIdAndStatus(user.id(), organizationId, MembershipStatus.ACTIVE)
                .orElseThrow(AccessService::notStaff);
        Role role = roles.findById(m.getRoleId()).orElseThrow(AccessService::notStaff);
        if (role.getRoleType() == RoleType.STUDENT || (role.getRoleType() == RoleType.CENTER && m.getCenterId() == null)) {
            throw notStaff();
        }
        return new StaffContext(user.id(), organizationId, role.getRoleType(),
                role.getRoleType() == RoleType.CENTER ? m.getCenterId() : null);
    }

    private static BusinessRuleException notStaff() {
        return new BusinessRuleException("NOT_STAFF", "Only staff of this academy can do this.");
    }

    /** Non-throwing check, safe to call inside a caller's transaction (a thrown exception would mark it rollback-only). */
    @Transactional(readOnly = true)
    public boolean can(UUID organizationId, AppModule module, AccessLevel level) {
        return find(organizationId).map(a -> a.can(module, level)).orElse(false);
    }

    private Optional<OrgAccess> find(UUID organizationId) {
        AuthUser user = currentUser.require();
        Optional<Organization> found = organizations.findById(organizationId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Organization org = found.get();
        if (user.platformAdmin()) {
            return Optional.of(new OrgAccess(org.getId(), user.id(), allManage(), null));
        }
        Optional<Membership> direct = memberships.findByUserIdAndOrganizationIdAndStatus(user.id(), org.getId(), MembershipStatus.ACTIVE);
        if (direct.isPresent()) {
            return toAccess(org.getId(), user.id(), direct.get());
        }
        if (org.getParentId() != null) {
            Optional<Membership> parent = memberships.findByUserIdAndOrganizationIdAndStatus(user.id(), org.getParentId(), MembershipStatus.ACTIVE);
            if (parent.isPresent()) {
                Optional<Role> role = roles.findWithPermissionsById(parent.get().getRoleId());
                if (role.isPresent() && role.get().getRoleType() == RoleType.HEAD_OFFICE) {
                    return Optional.of(new OrgAccess(org.getId(), user.id(), role.get().permissionView(), null));
                }
            }
        }
        return Optional.empty();
    }

    private Optional<OrgAccess> toAccess(UUID orgId, UUID userId, Membership m) {
        Optional<Role> found = roles.findWithPermissionsById(m.getRoleId());
        if (found.isEmpty() || found.get().getRoleType() == RoleType.STUDENT) {
            return Optional.empty();
        }
        Role role = found.get();
        UUID center = role.getRoleType() == RoleType.CENTER ? m.getCenterId() : null;
        if (role.getRoleType() == RoleType.CENTER && center == null) {
            // A center role without a center sees nothing rather than everything.
            return Optional.empty();
        }
        return Optional.of(new OrgAccess(orgId, userId, role.permissionView(), center));
    }

    static Map<AppModule, AccessLevel> allManage() {
        Map<AppModule, AccessLevel> all = new EnumMap<>(AppModule.class);
        for (AppModule m : AppModule.values()) all.put(m, AccessLevel.MANAGE);
        return all;
    }
}
