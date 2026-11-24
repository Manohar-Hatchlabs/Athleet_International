-- Stored files (selfies now; logos, post media and drills later) and the two attendance permission modules.

create table stored_file (
    id              uuid primary key,
    organization_id uuid         not null references organization (id),
    storage_key     varchar(200) not null unique,
    purpose         varchar(30)  not null,
    content_type    varchar(50)  not null,
    size_bytes      bigint       not null check (size_bytes > 0),
    sha256          varchar(64)  not null,
    uploaded_by     uuid         not null references app_user (id),
    attached_at     timestamptz,
    created_at      timestamptz  not null
);
create index ix_stored_file_org_created on stored_file (organization_id, created_at);
create index ix_stored_file_unattached on stored_file (created_at) where attached_at is null;

-- ATTENDANCE: CENTER roles that can see students may mark attendance; HEAD_OFFICE roles mirror their STUDENT level.
insert into role_permission (role_id, module, access_level)
select r.id, 'ATTENDANCE',
       case when r.role_type = 'CENTER' then 'MANAGE' else p.access_level end
from role r
join role_permission p on p.role_id = r.id and p.module = 'STUDENT' and p.access_level in ('VIEW', 'MANAGE')
where r.role_type in ('HEAD_OFFICE', 'CENTER')
  and not exists (select 1 from role_permission x where x.role_id = r.id and x.module = 'ATTENDANCE');

-- ATTENDANCE_APPROVAL: anyone who manages students can approve attendance.
insert into role_permission (role_id, module, access_level)
select r.id, 'ATTENDANCE_APPROVAL', 'MANAGE'
from role r
join role_permission p on p.role_id = r.id and p.module = 'STUDENT' and p.access_level = 'MANAGE'
where r.role_type in ('HEAD_OFFICE', 'CENTER')
  and not exists (select 1 from role_permission x where x.role_id = r.id and x.module = 'ATTENDANCE_APPROVAL');
