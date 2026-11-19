package com.athleet.core.organization.service;

import com.athleet.core.audit.service.AuditService;
import com.athleet.core.common.error.ConflictException;
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
import com.athleet.core.iam.service.AccessService;
import com.athleet.core.iam.service.DefaultRoles;
import com.athleet.core.iam.service.OrgAccess;
import com.athleet.core.organization.domain.Organization;
import com.athleet.core.organization.dto.OrganizationDtos.AccessibleOrganization;
import com.athleet.core.organization.dto.OrganizationDtos.BrandingRequest;
import com.athleet.core.organization.dto.OrganizationDtos.MyAccessResponse;
import com.athleet.core.organization.dto.OrganizationDtos.OrganizationRequest;
import com.athleet.core.organization.dto.OrganizationDtos.OrganizationResponse;
import com.athleet.core.organization.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrganizationService {

    private final OrganizationRepository organizations;
    private final MembershipRepository memberships;
    private final RoleRepository roles;
    private final AccessService access;
    private final CurrentUser currentUser;
    private final DefaultRoles defaultRoles;
    private final AuditService audit;

    public OrganizationService(OrganizationRepository organizations, MembershipRepository memberships, RoleRepository roles,
                               AccessService access, CurrentUser currentUser, DefaultRoles defaultRoles, AuditService audit) {
        this.organizations = organizations;
        this.memberships = memberships;
        this.roles = roles;
        this.access = access;
        this.currentUser = currentUser;
        this.defaultRoles = defaultRoles;
        this.audit = audit;
    }

    /** Organizations for the selector: primaries (with the affiliates the user can open) and directly-joined affiliates. */
    @Transactional(readOnly = true)
    public List<AccessibleOrganization> accessible() {
        AuthUser user = currentUser.require();
        List<Organization> primaries;
        Set<UUID> fullPrimaries; // primaries where the user sees every affiliate
        List<Organization> directAffiliates = new ArrayList<>();
        if (user.platformAdmin()) {
            primaries = organizations.findByParentIdIsNullOrderByNameAsc();
            fullPrimaries = primaries.stream().map(Organization::getId).collect(Collectors.toSet());
        } else {
            List<Membership> ms = memberships.findByUserIdAndStatus(user.id(), MembershipStatus.ACTIVE);
            Map<UUID, Role> roleById = roles.findByIdIn(ms.stream().map(Membership::getRoleId).toList()).stream()
                    .collect(Collectors.toMap(Role::getId, Function.identity()));
            List<Membership> staff = ms.stream()
                    .filter(m -> roleById.containsKey(m.getRoleId()) && roleById.get(m.getRoleId()).getRoleType() != RoleType.STUDENT)
                    .toList();
            List<Organization> orgs = organizations.findAllById(staff.stream().map(Membership::getOrganizationId).toList());
            primaries = orgs.stream().filter(Organization::isPrimary).sorted(Comparator.comparing(Organization::getName)).toList();
            fullPrimaries = staff.stream()
                    .filter(m -> roleById.get(m.getRoleId()).getRoleType() == RoleType.HEAD_OFFICE)
                    .map(Membership::getOrganizationId)
                    .collect(Collectors.toSet());
            orgs.stream().filter(o -> !o.isPrimary()).forEach(directAffiliates::add);
        }
        Map<UUID, List<Organization>> children = organizations
                .findByParentIdInOrderByNameAsc(primaries.stream().map(Organization::getId).filter(fullPrimaries::contains).toList())
                .stream().collect(Collectors.groupingBy(Organization::getParentId, LinkedHashMap::new, Collectors.toList()));
        List<AccessibleOrganization> result = new ArrayList<>();
        for (Organization p : primaries) {
            List<AccessibleOrganization> aff = children.getOrDefault(p.getId(), List.of()).stream().map(OrganizationService::leaf).toList();
            result.add(new AccessibleOrganization(p.getId(), p.getName(), p.getCity(), p.getCountry(), true, aff));
        }
        Set<UUID> listed = children.values().stream().flatMap(List::stream).map(Organization::getId).collect(Collectors.toSet());
        directAffiliates.stream().filter(o -> !listed.contains(o.getId()))
                .sorted(Comparator.comparing(Organization::getName))
                .forEach(o -> result.add(leaf(o)));
        return result;
    }

    @Transactional(readOnly = true)
    public MyAccessResponse myAccess(UUID orgId) {
        OrgAccess a = access.resolve(orgId);
        Organization org = organizations.findById(orgId).orElseThrow();
        int affiliates = org.isPrimary() ? organizations.findByParentIdOrderByNameAsc(orgId).size() : 0;
        return new MyAccessResponse(response(org), a.permissions(), a.centerId(), affiliates);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse get(UUID orgId) {
        access.resolve(orgId);
        return response(organizations.findById(orgId).orElseThrow());
    }

    /** Platform admins create primary organizations; head office of a primary creates affiliated academies. */
    @Transactional
    public OrganizationResponse create(UUID parentId, OrganizationRequest req) {
        AuthUser user = currentUser.require();
        String name = req.name().trim();
        if (parentId == null) {
            if (!user.platformAdmin()) throw new ForbiddenException("Only platform admins create primary organizations.");
            if (organizations.existsByParentIdIsNullAndNameIgnoreCase(name)) throw new ConflictException("An organization with this name already exists.");
        } else {
            OrgAccess a = access.require(parentId, AppModule.SETTINGS, AccessLevel.MANAGE);
            Organization parent = organizations.findById(parentId).orElseThrow();
            if (!parent.isPrimary()) throw new NotFoundException("Organization not found.");
            if (!a.allCenters()) throw new ForbiddenException("Center staff cannot add academies.");
            if (organizations.existsByParentIdAndNameIgnoreCase(parentId, name)) throw new ConflictException("An academy with this name already exists.");
        }
        Organization org = new Organization(parentId, name);
        apply(org, req);
        organizations.save(org);
        Role owner = defaultRoles.seed(org.getId());
        if (!user.platformAdmin()) {
            memberships.save(new Membership(user.id(), org.getId(), owner.getId(), null));
        }
        audit.record(org.getId(), user.id(), "ORGANIZATION", org.getId(), "CREATE", parentId == null ? "primary" : "affiliate of " + parentId);
        return response(org);
    }

    @Transactional
    public OrganizationResponse update(UUID orgId, OrganizationRequest req) {
        OrgAccess a = access.require(orgId, AppModule.SETTINGS, AccessLevel.MANAGE);
        Organization org = organizations.findById(orgId).orElseThrow();
        String name = req.name().trim();
        boolean clash = org.isPrimary()
                ? !name.equalsIgnoreCase(org.getName()) && organizations.existsByParentIdIsNullAndNameIgnoreCase(name)
                : !name.equalsIgnoreCase(org.getName()) && organizations.existsByParentIdAndNameIgnoreCase(org.getParentId(), name);
        if (clash) throw new ConflictException("An organization with this name already exists.");
        org.setName(name);
        apply(org, req);
        audit.record(orgId, a.userId(), "ORGANIZATION", orgId, "UPDATE", null);
        return response(org);
    }

    @Transactional
    public OrganizationResponse updateBranding(UUID orgId, BrandingRequest req) {
        OrgAccess a = access.require(orgId, AppModule.SETTINGS, AccessLevel.MANAGE);
        Organization org = organizations.findById(orgId).orElseThrow();
        org.setCenterLabel(req.centerLabel().trim());
        org.setCurrency(req.currency());
        org.setDateFormat(req.dateFormat());
        audit.record(orgId, a.userId(), "ORGANIZATION", orgId, "BRANDING", null);
        return response(org);
    }

    private static void apply(Organization o, OrganizationRequest r) {
        o.setCity(trim(r.city()));
        o.setState(trim(r.state()));
        o.setCountry(trim(r.country()));
        o.setAddress(trim(r.address()));
        o.setEmail(trim(r.email()));
        o.setPhone(trim(r.phone()));
        o.setSport(trim(r.sport()));
    }

    private static String trim(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static AccessibleOrganization leaf(Organization o) {
        return new AccessibleOrganization(o.getId(), o.getName(), o.getCity(), o.getCountry(), false, List.of());
    }

    public static OrganizationResponse response(Organization o) {
        return new OrganizationResponse(o.getId(), o.getParentId(), o.getName(), o.getCity(), o.getState(), o.getCountry(),
                o.getAddress(), o.getEmail(), o.getPhone(), o.getSport(), o.getStatus(), o.getCenterLabel(),
                o.getCurrency(), o.getDateFormat());
    }
}
