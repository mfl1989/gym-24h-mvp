create table processed_webhook_events (
    event_id varchar(255) primary key,
    event_type varchar(100) not null,
    processed_at timestamp with time zone not null
);