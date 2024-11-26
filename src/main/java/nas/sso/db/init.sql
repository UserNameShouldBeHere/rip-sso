create database auth;

create table if not exists users (
    id integer primary key generated always as identity,
    external_uuid uuid unique default gen_random_uuid() not null,
    name text unique check (length(name) > 3) not null,
    password text not null,
    registered_at timestamp default now() not null,
    version integer check (version >= 1) default 1 not null
);
