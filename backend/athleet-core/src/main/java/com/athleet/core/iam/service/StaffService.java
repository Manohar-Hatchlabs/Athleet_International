package com.athleet.core.iam.service;

import com.athleet.core.audit.service.AuditService;
import com.athleet.core.auth.service.PasswordService;
import com.athleet.core.common.error.BusinessRuleException;
import com.athleet.core.common.error.ConflictException;
import com.athleet.core.common.error.NotFoundException;
import com.athleet.core.common.paging.PageResponse;
import com.athleet.core.iam.domain.AccessLevel;
import com.athleet.core.iam.domain.AppModule;
import com.athleet.core.iam.domain.AppUser;
import com.athleet.core.iam.domain.Membership;
import com.athleet.core.iam.domain.MembershipStatus;
import com.athleet.core.iam.domain.Role;
import com.athleet.core.iam.domain.RoleType;
import com.athleet.core.iam.dto.StaffDtos.StaffRequest;
import com.athleet.core.iam.dto.StaffDtos.StaffResponse;
import com.athleet.core.iam.repository.AppUserRepository;
import com.athleet.core.iam.repository.MembershipRepository;
import com.athleet.core.iam.repository.RoleRepository;
import com.athleet.core.iam.repository.StaffQueryRepository;
import com.athleet.core.organization.domain.Organization;
import com.athleet.core.organization.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class StaffService {

    private final AccessService access;
    private final AppUserRepository users;
    private final MembershipRepository memberships;
    private final RoleRepository roles;
    private final StaffQueryRepository staffQuery;
    private final OrganizationRepository organizations;
    private final CenterDirectory centers;
    private final PasswordService passwords;
    private final AuditService audit;

    public StaffService(AccessService access, AppUserRepository users, MembershipRepository memberships, RoleRepository roles,
                        StaffQueryRepository staffQuery, OrganizationRepository organizations, CenterDirectory centers,
                        PasswordService passwords, AuditService audit) {
        this.access = access;
        this.users = users;
        this.memberships = memberships;
        this.roles = roles;
        this.staffQuery = staffQuery;
        this.organizations = organizations;
        this.centers = centers;
        this.passwords = passwords;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageResponse<StaffResponse> list(UUID orgId, String search, MembershipStatus status, UUID centerId, int page, int size) {
        OrgAccess a = access.require(orgId, AppModule.STAFF, AccessLevel.VIEW);
        UUID scopeCenter = a.allCenters() ? centerId : a.centerId();
        return staffQuery.search(orgId, search, status == null ? MembershipStatus.ACTIVE : status, scopeCenter, page, size);
    }

    @Transactional(readOnly = true)
    public StaffResponse get(UUID orgId, UUID membershipId) {
        OrgAccess a = access.require(orgId, AppModule.STAFF, AccessLevel.VIEW);
        StaffResponse s = staffQuery.findOne(orgId, membershipId).orElseThrow(() -> new NotFoundException("Staff member not found."));
        if (!a.allCenters() && !a.centerId().equals(s.centerId())) throw new NotFoundException("Staff member not found.");
        return s;
    }

    @Transactional
    public StaffResponse create(UUID orgId, StaffRequest req) {
        OrgAccess a = access.require(orgId, AppModule.STAFF, AccessLevel.MANAGE);
        Role role = validRole(orgId, req, a);
        String email = req.email().trim().toLowerCase(Locale.ROOT);

        AppUser user = users.findByEmailIgnoreCase(email).orElse(null);
        boolean newUser = user == null;
        if (newUser) {
            if (req.mobile() != null && users.findByCountryCodeAndMobile(req.countryCode(), req.mobile()).isPresent()) {
                throw new ConflictException("This mobile number belongs to another user.");
            }
            user = new AppUser(req.firstName().trim(), blankToNull(req.lastName()));
            user.setEmail(email);
            applyProfile(user, req);
            users.save(user);
        } else if (memberships.findByUserIdAndOrganizationId(user.getId(), orgId).isPresent()) {
            throw new ConflictException("This person is already a staff member here.");
        }
        Membership m = memberships.save(new Membership(user.getId(), orgId, role.getId(), centerFor(role, req)));
        audit.record(orgId, a.userId(), "STAFF", m.getId(), "CREATE", "user=" + user.getId() + " role=" + role.getId());
        if (newUser) {
            Organization org = organizations.findById(orgId).orElseThrow();
            passwords.sendInvite(user, org.getName());
        }
        return staffQuery.findOne(orgId, m.getId()).orElseThrow();
    }

    @Transactional
    public StaffResponse update(UUID orgId, UUID membershipId, StaffRequest req) {
        OrgAccess a = access.require(orgId, AppModule.STAFF, AccessLevel.MANAGE);
        Membership m = loadScoped(orgId, membershipId, a);
        Role role = validRole(orgId, req, a);
        AppUser user = users.findById(m.getUserId()).orElseThrow();
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (!email.equalsIgnoreCase(user.getEmail())) {
            if (users.findByEmailIgnoreCase(email).isPresent()) throw new ConflictException("This email belongs to another user.");
            user.setEmail(email);
        }
        if (req.mobile() != null) {
            users.findByCountryCodeAndMobile(req.countryCode(), req.mobile())
                    .filter(other -> !other.getId().equals(user.getId()))
                    .ifPresent(other -> { throw new ConflictException("This mobile number belongs to another user."); });
        }
        user.setFirstName(req.firstName().trim());
        user.setLastName(blankToNull(req.lastName()));
        applyProfile(user, req);
        String before = "role=" + m.getRoleId() + " center=" + m.getCenterId();
        m.setRoleId(role.getId());
        m.setCenterId(centerFor(role, req));
        audit.record(orgId, a.userId(), "STAFF", m.getId(), "UPDATE", before + " -> role=" + m.getRoleId() + " center=" + m.getCenterId());
        return staffQuery.findOne(orgId, m.getId()).orElseThrow();
    }

    @Transactional
    public void deregister(UUID orgId, UUID membershipId) {
        OrgAccess a = access.require(orgId, AppModule.STAFF, AccessLevel.MANAGE);
        Membership m = loadScoped(orgId, membershipId, a);
        if (m.getUserId().equals(a.userId())) {
            throw new BusinessRuleException("SELF_DEREGISTER", "You cannot deregister yourself.");
        }
        m.setStatus(MembershipStatus.DEREGISTERED);
        audit.record(orgId, a.userId(), "STAFF", m.getId(), "DEREGISTER", null);
    }

    private Role validRole(UUID orgId, StaffRequest req, OrgAccess a) {
        Role role = roles.findWithPermissionsByIdAndOrganizationId(req.roleId(), orgId)
                .orElseThrow(() -> new NotFoundException("Role not found."));
        if (role.getRoleType() == RoleType.STUDENT) {
            throw new BusinessRuleException("INVALID_ROLE", "Student roles cannot be given to staff.");
        }
        // No escalation: you can only assign roles whose permissions you hold yourself.
        role.permissionView().forEach((module, level) -> {
            if (!a.can(module, level)) {
                throw new BusinessRuleException("PERMISSION_ESCALATION", "You cannot assign a role with more access than your own.");
            }
        });
        if (!a.allCenters() && role.getRoleType() == RoleType.HEAD_OFFICE) {
            throw new BusinessRuleException("PERMISSION_ESCALATION", "Center staff cannot assign head office roles.");
        }
        return role;
    }

    private UUID centerFor(Role role, StaffRequest req) {
        if (role.getRoleType() != RoleType.CENTER) return null;
        if (req.centerId() == null) {
            throw new BusinessRuleException("CENTER_REQUIRED", "Select the center for this role.");
        }
        if (!centers.existsInOrganization(req.centerId(), role.getOrganizationId())) {
            throw new NotFoundException("Center not found.");
        }
        return req.centerId();
    }

    private Membership loadScoped(UUID orgId, UUID membershipId, OrgAccess a) {
        Membership m = memberships.findByIdAndOrganizationId(membershipId, orgId)
                .orElseThrow(() -> new NotFoundException("Staff member not found."));
        if (!a.allCenters() && !a.centerId().equals(m.getCenterId())) {
            throw new NotFoundException("Staff member not found.");
        }
        return m;
    }

    private static void applyProfile(AppUser u, StaffRequest req) {
        u.setCountryCode(req.mobile() == null ? null : req.countryCode());
        u.setMobile(req.mobile());
        u.setDateOfJoining(req.dateOfJoining());
        u.setQualifications(blankToNull(req.qualifications()));
        u.setProfile(blankToNull(req.profile()));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
