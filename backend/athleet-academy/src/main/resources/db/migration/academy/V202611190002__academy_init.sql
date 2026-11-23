-- Academy: classes (centers), batches with weekly schedule and coaches, students.

create table center (
    id                   uuid primary key,
    version              bigint       not null,
    created_at           timestamptz  not null,
    updated_at           timestamptz  not null,
    organization_id      uuid         not null references organization (id),
    name                 varchar(100) not null,
    code                 varchar(30),
    contact_name         varchar(100),
    contact_country_code varchar(5),
    contact_phone        varchar(15),
    contact_email        varchar(254),
    sport                varchar(60),
    facility_type        varchar(60),
    facility_count       integer,
    status               varchar(10)  not null,
    country              varchar(100),
    state                varchar(100),
    city                 varchar(100),
    address1             varchar(200),
    address2             varchar(200),
    zipcode              varchar(12),
    latitude             numeric(9, 6),
    longitude            numeric(9, 6),
    geofence_radius_m    integer      not null default 200,
    constraint ck_center_status check (status in ('ACTIVE', 'INACTIVE'))
);
create unique index ux_center_org_name on center (organization_id, lower(name));

alter table membership add constraint fk_membership_center foreign key (center_id) references center (id);

create table batch (
    id              uuid primary key,
    version         bigint       not null,
    created_at      timestamptz  not null,
    updated_at      timestamptz  not null,
    organization_id uuid         not null references organization (id),
    center_id       uuid         not null references center (id),
    name            varchar(100) not null,
    start_date      date,
    end_date        date,
    capacity        integer      not null check (capacity > 0),
    status          varchar(10)  not null,
    description     varchar(500),
    constraint ck_batch_status check (status in ('ACTIVE', 'INACTIVE')),
    constraint ck_batch_dates check (end_date is null or start_date is null or end_date >= start_date)
);
create unique index ux_batch_center_name on batch (center_id, lower(name));
create index ix_batch_org on batch (organization_id);

create table batch_schedule (
    batch_id    uuid        not null references batch (id) on delete cascade,
    day_of_week varchar(10) not null,
    start_time  time        not null,
    end_time    time        not null,
    constraint ck_batch_schedule_time check (end_time > start_time),
    constraint pk_batch_schedule primary key (batch_id, day_of_week)
);

create table batch_coach (
    batch_id uuid not null references batch (id) on delete cascade,
    user_id  uuid not null references app_user (id),
    constraint pk_batch_coach primary key (batch_id, user_id)
);
create index ix_batch_coach_user on batch_coach (user_id);

create sequence student_no_seq start with 100001;

create table student (
    id                    uuid primary key,
    version               bigint       not null,
    created_at            timestamptz  not null,
    updated_at            timestamptz  not null,
    organization_id       uuid         not null references organization (id),
    student_no            bigint       not null unique,
    center_id             uuid         not null references center (id),
    user_id               uuid references app_user (id),
    first_name            varchar(60)  not null,
    last_name             varchar(60),
    email                 varchar(254),
    gender                varchar(10),
    date_of_birth         date,
    country_code          varchar(5),
    mobile                varchar(15),
    unique_id             varchar(40),
    blood_group           varchar(3),
    country               varchar(100),
    state                 varchar(100),
    city                  varchar(100),
    address               varchar(300),
    pin_code              varchar(12),
    school_name           varchar(150),
    school_address        varchar(300),
    grade                 varchar(20),
    dominant_side         varchar(5),
    medical_condition     varchar(1000),
    status                varchar(15)  not null,
    deregistered_at       timestamptz,
    deregistration_reason varchar(500),
    constraint ck_student_status check (status in ('ACTIVE', 'SUBMITTED', 'DEREGISTERED')),
    constraint ck_student_gender check (gender is null or gender in ('MALE', 'FEMALE', 'OTHER')),
    constraint ck_student_side check (dominant_side is null or dominant_side in ('LEFT', 'RIGHT', 'BOTH'))
);
create index ix_student_org_status on student (organization_id, status);
create index ix_student_center_status on student (center_id, status);
create index ix_student_org_email on student (organization_id, lower(email));

create table student_batch (
    student_id uuid not null references student (id) on delete cascade,
    batch_id   uuid not null references batch (id),
    constraint pk_student_batch primary key (student_id, batch_id)
);
create index ix_student_batch_batch on student_batch (batch_id);
