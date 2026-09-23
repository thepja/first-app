create table app_user (
    id            bigint generated always as identity primary key,
    email         varchar(254) not null unique check (email = lower(email)),
    password_hash varchar(100) not null,
    display_name  varchar(80)  not null,
    created_at    timestamptz  not null default now()
);

create table book (
    id         bigint generated always as identity primary key,
    owner_id   bigint       not null references app_user (id) on delete cascade,
    title      varchar(200) not null,
    author     varchar(200),
    read_on    date,
    rating     smallint     not null check (rating between 1 and 5),
    comment    varchar(5000),
    created_at timestamptz  not null default now(),
    updated_at timestamptz  not null default now()
);

create index book_owner_idx on book (owner_id);
