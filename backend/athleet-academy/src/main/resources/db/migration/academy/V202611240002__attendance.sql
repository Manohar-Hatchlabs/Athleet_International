-- Attendance: per-organization settings, student attendance sessions (approval workflow) and staff in/out shifts.

create table attendance_config (
    id                  uuid primary key,
    version             bigint      not null,
    created_at          timestamptz not null,
    updated_at          timestamptz not null,
    organization_id     uuid        not null unique references organization (id),
    selfie_required     boolean     not null default false,
    tasks_on_out        boolean     not null default false,
    show_device_info    boolean     not null default false,
    allow_date_override boolean     not null default false,
    max_back_days       integer     not null default 0 check (max_back_days between 0 and 31),
    max_forward_days    integer     not null default 0 check (max_forward_days between 0 and 7)
);

create table attendance_session (
    id              uuid primary key,
    version         bigint       not null,
    created_at      timestamptz  not null,
    updated_at      timestamptz  not null,
    organization_id uuid         not null references organization (id),
    center_id       uuid         not null references center (id),
    batch_id        uuid         not null references batch (id),
    session_date    date         not null,
    status          varchar(12)  not null,
    created_by      uuid         not null references app_user (id),
    marked_by       uuid references app_user (id),
    marked_at       timestamptz,
    submitted_by    uuid references app_user (id),
    submitted_at    timestamptz,
    reviewed_by     uuid references app_user (id),
    reviewed_at     timestamptz,
    review_reason   varchar(500),
    constraint ck_attendance_session_status check (status in ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'RESUBMITTED')),
    constraint ux_attendance_session_batch_date unique (batch_id, session_date)
);
create index ix_att_session_org_date on attendance_session (organization_id, session_date);
create index ix_att_session_center_date on attendance_session (center_id, session_date);
create index ix_att_session_pending on attendance_session (organization_id, session_date)
    where status in ('SUBMITTED', 'RESUBMITTED');

create table attendance_mark (
    id         uuid primary key,
    version    bigint      not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    session_id uuid        not null references attendance_session (id) on delete cascade,
    student_id uuid        not null references student (id),
    status     varchar(10),
    remark     varchar(200),
    constraint ck_attendance_mark_status check (status is null or status in ('PRESENT', 'ABSENT')),
    constraint ux_attendance_mark unique (session_id, student_id)
);
create index ix_att_mark_student on attendance_mark (student_id);

create table staff_attendance (
    id                   uuid primary key,
    version              bigint      not null,
    created_at           timestamptz not null,
    updated_at           timestamptz not null,
    organization_id      uuid        not null references organization (id),
    user_id              uuid        not null references app_user (id),
    center_id            uuid references center (id),
    batch_id             uuid references batch (id),
    work_date            date        not null,
    in_at                timestamptz not null,
    out_at               timestamptz,
    source               varchar(10) not null,
    in_client_id         uuid,
    out_client_id        uuid,
    in_latitude          numeric(9, 6),
    in_longitude         numeric(9, 6),
    in_accuracy_m        integer,
    in_distance_m        integer,
    in_outside_geofence  boolean,
    out_latitude         numeric(9, 6),
    out_longitude        numeric(9, 6),
    out_accuracy_m       integer,
    out_distance_m       integer,
    out_outside_geofence boolean,
    in_selfie_file_id    uuid references stored_file (id),
    out_selfie_file_id   uuid references stored_file (id),
    device_id            varchar(100),
    phone_type           varchar(60),
    tasks_accomplished   varchar(1000),
    corrected_by         uuid references app_user (id),
    corrected_at         timestamptz,
    correction_reason    varchar(300),
    constraint ck_staff_att_source check (source in ('WEB', 'APP', 'ADMIN')),
    constraint ck_staff_att_out check (out_at is null or out_at >= in_at),
    constraint ck_staff_att_in_lat check (in_latitude is null or in_latitude between -90 and 90),
    constraint ck_staff_att_in_lng check (in_longitude is null or in_longitude between -180 and 180),
    constraint ck_staff_att_out_lat check (out_latitude is null or out_latitude between -90 and 90),
    constraint ck_staff_att_out_lng check (out_longitude is null or out_longitude between -180 and 180)
);
create unique index ux_staff_att_in_client on staff_attendance (organization_id, user_id, in_client_id)
    where in_client_id is not null;
create unique index ux_staff_att_out_client on staff_attendance (organization_id, user_id, out_client_id)
    where out_client_id is not null;
create unique index ux_staff_att_open_day on staff_attendance (organization_id, user_id, work_date)
    where out_at is null;
create index ix_staff_att_org_date on staff_attendance (organization_id, work_date);
create index ix_staff_att_center_date on staff_attendance (center_id, work_date);
create index ix_staff_att_user_date on staff_attendance (user_id, work_date);
