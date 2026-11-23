-- Core platform: organizations, users, roles/permissions, staff memberships, auth tokens, audit log.

create table organization (
    id            uuid primary key,
    parent_id     uuid references organization (id),
    name          varchar(150) not null,
    city          varchar(100),
    state         varchar(100),
    country       varchar(100),
    address       varchar(300),
    email         varchar(254),
    phone         varchar(20),
    sport         varchar(60),
    logo_key      varchar(200),
    status        varchar(20)  not null,
    center_label  varchar(30)  not null default 'Center',
    currency      varchar(3)   not null default 'INR',
    date_format   varchar(20)  not null default 'd/m/Y',
    version       bigint       not null default 0,
    created_at    timestamptz  not null,
    updated_at    timestamptz  not null
);
create index ix_organization_parent on organization (parent_id);
create unique index ux_organization_primary_name on organization (lower(name)) where parent_id is null;
create unique index ux_organization_affiliate_name on organization (parent_id, lower(name)) where parent_id is not null;

create table app_user (
    id                  uuid primary key,
    email               varchar(254),
    country_code        varchar(5),
    mobile              varchar(15),
    password_hash       varchar(100),
    first_name          varchar(60)  not null,
    last_name           varchar(60),
    platform_admin      boolean      not null default false,
    status              varchar(20)  not null,
    date_of_joining     date,
    qualifications      varchar(300),
    profile             varchar(1000),
    password_changed_at timestamptz,
    version             bigint       not null default 0,
    created_at          timestamptz  not null,
    updated_at          timestamptz  not null
);
create unique index ux_app_user_email on app_user (lower(email)) where email is not null;
create unique index ux_app_user_mobile on app_user (country_code, mobile) where mobile is not null;
create index ix_app_user_mobile on app_user (mobile);

create table role (
    id              uuid primary key,
    organization_id uuid        not null references organization (id),
    name            varchar(60) not null,
    role_type       varchar(20) not null,
    system_role     boolean     not null default false,
    version         bigint      not null default 0,
    created_at      timestamptz not null,
    updated_at      timestamptz not null
);
create unique index ux_role_org_name on role (organization_id, lower(name));

create table role_permission (
    role_id      uuid        not null references role (id) on delete cascade,
    module       varchar(30) not null,
    access_level varchar(10) not null,
    primary key (role_id, module)
);

create table membership (
    id              uuid primary key,
    user_id         uuid        not null references app_user (id),
    organization_id uuid        not null references organization (id),
    role_id         uuid        not null references role (id),
    center_id       uuid,
    status          varchar(20) not null,
    version         bigint      not null default 0,
    created_at      timestamptz not null,
    updated_at      timestamptz not null
);
create unique index ux_membership_user_org on membership (user_id, organization_id);
create index ix_membership_org_status on membership (organization_id, status);
create index ix_membership_role on membership (role_id);
create index ix_membership_center on membership (center_id);

create table refresh_token (
    id         uuid primary key,
    user_id    uuid        not null references app_user (id),
    token_hash varchar(64) not null unique,
    expires_at timestamptz not null,
    revoked_at timestamptz,
    created_at timestamptz not null
);
create index ix_refresh_token_user on refresh_token (user_id);
create index ix_refresh_token_expires on refresh_token (expires_at);

create table password_token (
    id         uuid primary key,
    user_id    uuid        not null references app_user (id),
    token_hash varchar(64) not null unique,
    expires_at timestamptz not null,
    used_at    timestamptz,
    created_at timestamptz not null
);
create index ix_password_token_user on password_token (user_id);

create table audit_log (
    id              uuid primary key,
    organization_id uuid,
    actor_user_id   uuid,
    entity_type     varchar(40)  not null,
    entity_id       uuid,
    action          varchar(40)  not null,
    details         varchar(2000),
    created_at      timestamptz  not null
);
create index ix_audit_log_org_created on audit_log (organization_id, created_at desc, id);
create index ix_audit_log_entity on audit_log (entity_type, entity_id);
