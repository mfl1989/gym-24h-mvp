alter table invoices add column if not exists user_id uuid;
alter table invoices add column if not exists invoice_url varchar(1000);

update invoices
set user_id = (
    select subscriptions.user_id
    from subscriptions
    where subscriptions.id = invoices.subscription_id
)
where user_id is null;

create index if not exists idx_invoices_user_status on invoices (user_id, status);
