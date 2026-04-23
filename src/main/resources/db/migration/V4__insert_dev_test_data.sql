insert into users (
    id,
    line_user_id,
    phone_number,
    display_name,
    membership_status,
    created_at,
    updated_at,
    version,
    is_deleted
)
select
    '11111111-1111-1111-1111-111111111111'::uuid,
    'line-dev-11111111-1111-1111-1111-111111111111',
    null,
    '孟凡龍',
    'ACTIVE',
    now(),
    now(),
    0,
    false
where not exists (
    select 1
    from users
    where id = '11111111-1111-1111-1111-111111111111'::uuid
);

insert into subscriptions (
    id,
    user_id,
    plan_code,
    billing_cycle,
    status,
    stripe_customer_id,
    stripe_subscription_id,
    started_at,
    current_period_start_at,
    current_period_end_at,
    canceled_at,
    cancellation_requested_at,
    created_at,
    updated_at,
    version,
    is_deleted
)
select
    '22222222-2222-2222-2222-222222222222'::uuid,
    '11111111-1111-1111-1111-111111111111'::uuid,
    'STANDARD',
    'THIRTY_DAYS',
    'ACTIVE',
    null,
    'sub_dev_test',
    now(),
    now(),
    now() + interval '30' day,
    null,
    null,
    now(),
    now(),
    0,
    false
where exists (
    select 1
    from users
    where id = '11111111-1111-1111-1111-111111111111'::uuid
)
and not exists (
    select 1
    from subscriptions
    where id = '22222222-2222-2222-2222-222222222222'::uuid
);

insert into invoices (
    id,
    subscription_id,
    user_id,
    stripe_invoice_id,
    stripe_event_id,
    amount,
    currency,
    status,
    invoice_url,
    billed_at,
    paid_at,
    due_at,
    created_at,
    updated_at,
    version,
    is_deleted
)
select
    '33333333-3333-3333-3333-333333333333'::uuid,
    '22222222-2222-2222-2222-222222222222'::uuid,
    '11111111-1111-1111-1111-111111111111'::uuid,
    'in_dev_test_001',
    'evt_dev_test_001',
    298000,
    'jpy',
    'PAID',
    'https://example.com/invoices/in_dev_test_001',
    now(),
    now(),
    now() + interval '30' day,
    now(),
    now(),
    0,
    false
where exists (
    select 1
    from subscriptions
    where id = '22222222-2222-2222-2222-222222222222'::uuid
)
and not exists (
    select 1
    from invoices
    where id = '33333333-3333-3333-3333-333333333333'::uuid
);