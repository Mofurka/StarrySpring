create table players
(
    id         uuid primary key,
    username   varchar(64)              not null unique,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);