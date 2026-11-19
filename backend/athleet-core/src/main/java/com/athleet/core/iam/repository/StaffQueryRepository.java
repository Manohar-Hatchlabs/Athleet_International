package com.athleet.core.iam.repository;

import com.athleet.core.common.paging.PageResponse;
import com.athleet.core.common.paging.Pages;
import com.athleet.core.iam.domain.MembershipStatus;
import com.athleet.core.iam.domain.RoleType;
import com.athleet.core.iam.dto.StaffDtos.StaffResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Staff list as a single JPQL projection (membership + user + role), always filtered by organization. */
@Repository
public class StaffQueryRepository {

    private static final String SELECT = """
            select m.id, u.id, u.firstName, u.lastName, u.email, u.countryCode, u.mobile, r.id, r.name, r.roleType,
                   m.centerId, m.status, u.dateOfJoining, u.qualifications, u.profile,
                   case when u.passwordHash is null then false else true end
            from Membership m, AppUser u, Role r
            where u.id = m.userId and r.id = m.roleId and m.organizationId = :orgId
            """;

    private final EntityManager em;

    public StaffQueryRepository(EntityManager em) {
        this.em = em;
    }

    public PageResponse<StaffResponse> search(UUID orgId, String search, MembershipStatus status, UUID centerId, int page, int size) {
        StringBuilder where = new StringBuilder(" and m.status = :status");
        if (centerId != null) where.append(" and m.centerId = :centerId");
        String like = null;
        if (search != null && !search.isBlank()) {
            like = "%" + search.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            where.append(" and (lower(concat(u.firstName, ' ', coalesce(u.lastName, ''))) like :like or lower(u.email) like :like or u.mobile like :like)");
        }
        int s = Math.min(Math.max(size, 1), Pages.MAX_SIZE);
        int p = Math.max(page, 0);
        TypedQuery<Object[]> q = em.createQuery(SELECT + where + " order by u.firstName, u.lastName, m.id", Object[].class);
        TypedQuery<Long> count = em.createQuery("select count(m) from Membership m, AppUser u where u.id = m.userId and m.organizationId = :orgId" + where, Long.class);
        for (var query : List.of(q, count)) {
            query.setParameter("orgId", orgId);
            query.setParameter("status", status);
            if (centerId != null) query.setParameter("centerId", centerId);
            if (like != null) query.setParameter("like", like);
        }
        List<StaffResponse> items = q.setFirstResult(p * s).setMaxResults(s).getResultList().stream().map(StaffQueryRepository::map).toList();
        return new PageResponse<>(items, p, s, count.getSingleResult());
    }

    public Optional<StaffResponse> findOne(UUID orgId, UUID membershipId) {
        return em.createQuery(SELECT + " and m.id = :id", Object[].class)
                .setParameter("orgId", orgId).setParameter("id", membershipId)
                .getResultStream().findFirst().map(StaffQueryRepository::map);
    }

    private static StaffResponse map(Object[] r) {
        return new StaffResponse((UUID) r[0], (UUID) r[1], (String) r[2], (String) r[3], (String) r[4], (String) r[5],
                (String) r[6], (UUID) r[7], (String) r[8], (RoleType) r[9], (UUID) r[10], (MembershipStatus) r[11],
                (LocalDate) r[12], (String) r[13], (String) r[14], (Boolean) r[15]);
    }
}
