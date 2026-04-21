create table users (
    id uuid primary key,
    line_user_id varchar(50) not null,
    phone_number varchar(50),
    display_name varchar(255),
    membership_status varchar(50) not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    version int not null default 0,
    is_deleted boolean not null default false,
    sub_char1 varchar(255) default null,
    sub_char2 varchar(255) default null,
    sub_char3 varchar(255) default null,
    sub_char4 varchar(255) default null,
    sub_char5 varchar(255) default null,
    sub_char6 varchar(255) default null,
    sub_char7 varchar(255) default null,
    sub_char8 varchar(255) default null,
    sub_char9 varchar(255) default null,
    sub_char10 varchar(255) default null,
    constraint uk_users_line_user_id unique (line_user_id)
);

create table subscriptions (
    id uuid primary key,
    user_id uuid not null,
    plan_code varchar(50) not null,
    billing_cycle varchar(50) not null,
    status varchar(50) not null,
    stripe_customer_id varchar(255),
    stripe_subscription_id varchar(255),
    started_at timestamp with time zone not null,
    current_period_start_at timestamp with time zone not null,
    current_period_end_at timestamp with time zone not null,
    canceled_at timestamp with time zone,
    cancellation_requested_at timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    version int not null default 0,
    is_deleted boolean not null default false,
    sub_char1 varchar(255) default null,
    sub_char2 varchar(255) default null,
    sub_char3 varchar(255) default null,
    sub_char4 varchar(255) default null,
    sub_char5 varchar(255) default null,
    sub_char6 varchar(255) default null,
    sub_char7 varchar(255) default null,
    sub_char8 varchar(255) default null,
    sub_char9 varchar(255) default null,
    sub_char10 varchar(255) default null,
    constraint fk_subscriptions_user foreign key (user_id) references users (id),
    constraint uk_subscriptions_stripe_subscription_id unique (stripe_subscription_id)
);

create table invoices (
    id uuid primary key,
    subscription_id uuid not null,
    stripe_invoice_id varchar(255) not null,
    stripe_event_id varchar(255) not null,
    amount integer not null,
    currency varchar(10) not null,
    status varchar(50) not null,
    billed_at timestamp with time zone not null,
    paid_at timestamp with time zone,
    due_at timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    version int not null default 0,
    is_deleted boolean not null default false,
    sub_char1 varchar(255) default null,
    sub_char2 varchar(255) default null,
    sub_char3 varchar(255) default null,
    sub_char4 varchar(255) default null,
    sub_char5 varchar(255) default null,
    sub_char6 varchar(255) default null,
    sub_char7 varchar(255) default null,
    sub_char8 varchar(255) default null,
    sub_char9 varchar(255) default null,
    sub_char10 varchar(255) default null,
    constraint fk_invoices_subscription foreign key (subscription_id) references subscriptions (id),
    constraint uk_invoices_stripe_invoice_id unique (stripe_invoice_id),
    constraint uk_invoices_stripe_event_id unique (stripe_event_id)
);

create table audit_logs (
    id uuid primary key,
    user_id uuid,
    action varchar(100) not null,
    result varchar(100) not null,
    reason varchar(500),
    request_id varchar(100),
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    version int not null default 0,
    is_deleted boolean not null default false,
    constraint fk_audit_logs_user foreign key (user_id) references users (id)
);

create index idx_subscriptions_user_status on subscriptions (user_id, status);
create index idx_invoices_subscription_status on invoices (subscription_id, status);
create index idx_invoices_stripe_event_id on invoices (stripe_event_id);
create index idx_audit_logs_user_action on audit_logs (user_id, action);

comment on column users.created_at is 'UTC timestamp';
comment on column users.updated_at is 'UTC timestamp';
comment on column subscriptions.started_at is 'UTC timestamp';
comment on column subscriptions.current_period_start_at is 'UTC timestamp';
comment on column subscriptions.current_period_end_at is 'UTC timestamp';
comment on column subscriptions.created_at is 'UTC timestamp';
comment on column subscriptions.updated_at is 'UTC timestamp';
comment on column invoices.billed_at is 'UTC timestamp';
comment on column invoices.paid_at is 'UTC timestamp';
comment on column invoices.due_at is 'UTC timestamp';
comment on column invoices.created_at is 'UTC timestamp';
comment on column invoices.updated_at is 'UTC timestamp';
comment on column audit_logs.created_at is 'UTC timestamp';
comment on column audit_logs.updated_at is 'UTC timestamp';

