create sequence user_id_seq start with 100 increment by 50;

create table "user"
(
    id         bigint       not null default nextval('user_id_seq'),
    email      varchar(255) not null,
    password   varchar(255) not null,
    name       varchar(255) not null,
    role       varchar(20)  not null,
    created_at timestamp    not null,
    updated_at timestamp,
    version    bigint       not null default 0,
    primary key (id),
    constraint user_email_unique unique (email)
);

create sequence post_id_seq start with 100 increment by 50;

create table post
(
    id         bigint       not null default nextval('post_id_seq'),
    title      varchar(250) not null,
    slug       varchar(300) not null,
    content    text         not null,
    created_by bigint       not null references "user" (id),
    updated_by bigint references "user" (id),
    created_at timestamp    not null,
    updated_at timestamp,
    version    bigint       not null default 0,
    primary key (id),
    constraint post_slug_unique unique (slug)
);

create sequence comment_id_seq start with 100 increment by 50;

create table comment
(
    id         bigint       not null default nextval('comment_id_seq'),
    post_id    bigint       not null references post (id),
    name       varchar(150) not null,
    email      varchar(150),
    content    text         not null,
    created_at timestamp    not null,
    updated_at timestamp,
    version    bigint       not null default 0,
    primary key (id)
);
