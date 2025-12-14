create table if not exists inventory
(
    product_id varchar(64) primary key,
    available  int not null,
    reserved   int not null
) engine=InnoDB default charset=utf8mb4;

create table if not exists outbox
(
    id           bigint primary key auto_increment,
    aggregate_id varchar(64) not null,
    event_type   varchar(64) not null,
    payload      longtext    not null,
    status       varchar(16) not null,
    retries      int         not null default 0,
    error        longtext    null,
    created_at   timestamp   not null default current_timestamp,
    sent_at      timestamp   null,
    key          idx_outbox_status_id (status, id)
) engine=InnoDB default charset=utf8mb4;

create table if not exists processed_messages
(
    message_key  varchar(128) primary key,
    processed_at timestamp not null default current_timestamp
) engine=InnoDB default charset=utf8mb4;

