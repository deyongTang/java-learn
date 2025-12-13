create database if not exists order_db default character set utf8mb4 collate utf8mb4_0900_ai_ci;
create database if not exists inventory_db default character set utf8mb4 collate utf8mb4_0900_ai_ci;

-- 可选：创建 demo 账号（与两个服务的默认配置一致）
create user if not exists 'demo'@'%' identified by 'demo';
grant all privileges on order_db.* to 'demo'@'%';
grant all privileges on inventory_db.* to 'demo'@'%';
flush privileges;

