# ShardingSphere-JDBC 学习案例

这是一个最小可跑的 ShardingSphere-JDBC（原 Sharding-JDBC）学习案例，使用 H2 内存库演示逻辑表 `t_order` 如何按 `order_id` 分片到 `t_order_0` 和 `t_order_1`。

## ShardingSphere-JDBC 是什么

ShardingSphere-JDBC 是一个嵌入式的数据库中间件。它以 **JDBC 驱动** 的形式运行在应用进程内，帮你在不改业务 SQL 的情况下完成分库分表、读写分离、分布式主键等能力。

简单理解：**应用仍然写一张“逻辑表”的 SQL，ShardingSphere-JDBC 在运行时把 SQL 路由到正确的真实表/库。**

```
               +--------------------+
Request/SQL -> |  ShardingSphere    |
               |  JDBC (in-app)     |
               +----------+---------+
                          |
                          | route/merge SQL
                          v
                +---------+---------+
                |   DataSources     |
                |  (DB nodes)       |
                +---+-----------+---+
                    |           |
                    v           v
               t_order_0     t_order_1
```

## 用来做什么

- 分库分表：数据量大时按规则拆表/拆库，提高单表性能上限。
- 读写分离：写走主库，读走从库（本例提供演示 profile）。
- 分布式主键：统一生成全局唯一 id（本例为了简单使用自增 id）。
- 透明迁移：业务 SQL 基本不改动，降低改造成本。

## 企业里常见的分库思路（参考）

下面是企业里相对常见、可落地的分库分表策略与约束，用来帮助你理解为什么要分、怎么分、怎么演进。

### 常见目标

- 单库容量上限与性能上限（单表行数、索引体积、写入 TPS）。
- 读写隔离与故障隔离（热点库独立、核心库优先级更高）。
- 扩容成本可控（避免每次扩容都大迁移）。

### 典型分库维度

1) 按业务域拆库（优先）
- 订单库、库存库、支付库、营销库。  
- 优点：边界清晰、团队协作成本低。

2) 按用户维度分库（常见）
- `user_id` 取模路由到不同库。
- 优点：用户相关数据聚合，热点分散。
- 适合：用户维度强的业务（订单/账单/消息）。

3) 按时间维度分库/分表（大表常用）
- 以月/季度分表，冷数据归档。
- 优点：冷热分离、便于历史清理。

4) 组合分片（企业常见）
- 先按业务库拆分，再按用户维度/时间维度细分。
- 需要权衡跨库查询与聚合成本。

### 选择分片键的经验

- 优先选择 **高基数、分布均匀** 的字段（如 `user_id`）。
- 尽量让主要查询条件包含分片键，否则会广播查询。
- 避免经常变更的字段作为分片键（迁移成本高）。

### 扩容与迁移策略（企业最关心）

- 预留分片位：一开始设置较多逻辑分片，后续只需调整映射表。
- 平滑扩容：双写/回放/对账，逐步切流。
- 迁移工具：自研迁移程序或数据中间层。

### 读写与一致性

- 读写分离需要处理延迟一致性（读到旧数据）。
- 跨库事务成本高，业务层需尽量避免强一致跨库写。

### 观测与治理

- SQL 路由日志、慢查询、分片倾斜监控。
- 热点库告警（单库 QPS/CPU/IO）。

### 一句话总结

先 **按业务拆库**，再 **按用户或时间细分**。分片键要匹配主查询路径，扩容要考虑“低成本迁移”。

## 核心概念（本例）

- 逻辑表：`t_order`（业务 SQL 里的表名）
- 真实表：`t_order_0`、`t_order_1`
- 分片键：`order_id`
- 分片算法：`order_id % 2`

所以：
```
order_id % 2 = 0 -> t_order_0
order_id % 2 = 1 -> t_order_1
```

## 运行

本示例基于 Spring Boot 2.7.x，建议使用 JDK 17 运行。

```bash
./mvnw -f sharding-jdbc-demo/pom.xml spring-boot:run
```

启动后会看到 ShardingSphere 打印实际路由 SQL（`sql-show: true`）。

## 示例请求

创建订单（不传 `orderId` 会在服务内生成一个简单的自增 id，仅用于演示）：

```bash
curl -X POST http://localhost:8081/orders \
  -H 'Content-Type: application/json' \
  -d '{"userId": 1001, "amount": 99.5}'
```

按 orderId 查询：

```bash
curl http://localhost:8081/orders/1001
```

按 userId 列表：

```bash
curl "http://localhost:8081/orders?userId=1001&limit=5"
```

## 关键配置

`src/main/resources/application.yml` 中：

- `actual-data-nodes: ds0.t_order_$->{0..1}`：逻辑表 `t_order` 映射到两个物理表。
- `algorithm-expression: t_order_$->{order_id % 2}`：按 `order_id` 取模路由。
- `sql-show: true`：打印实际执行的 SQL。

## 进阶案例（企业常见形态）

下面几个 profile 是可直接运行的教学配置（默认仍是单库分表）。

### 0) 自定义分表算法（CLASS_BASED）

运行：

```bash
./mvnw -f sharding-jdbc-demo/pom.xml spring-boot:run -Dspring-boot.run.profiles=custom
```

配置见：`src/main/resources/application-custom.yml`  
算法类：`src/main/java/com/example/shardingdemo/sharding/OrderTableShardingAlgorithm.java`

算法逻辑（简化版）：
```
suffix = abs(order_id) % table-count
table  = t_order_{suffix}
```

范围查询（between）会路由到所有分表，避免漏数据。

### 0.1) SPI 自定义分表算法

运行：

```bash
./mvnw -f sharding-jdbc-demo/pom.xml spring-boot:run -Dspring-boot.run.profiles=spi
```

配置见：`src/main/resources/application-spi.yml`  
SPI 实现：`src/main/java/com/example/shardingdemo/sharding/OrderTableSpiShardingAlgorithm.java`  
SPI 注册：`src/main/resources/META-INF/services/org.apache.shardingsphere.sharding.spi.ShardingAlgorithm`

算法逻辑与 CLASS_BASED 一致，只是通过 SPI 注册，配置里直接用 `type: ORDER_TABLE_SPI`。

### 1) 组合分片：按用户分库 + 按订单分表

运行：

```bash
./mvnw -f sharding-jdbc-demo/pom.xml spring-boot:run -Dspring-boot.run.profiles=combo
```

配置见：`src/main/resources/application-combo.yml`

规则：
```
user_id % 2 -> ds0 / ds1
order_id % 2 -> t_order_0 / t_order_1
```

```
Request
  |
  v
Router -> ds0 -> t_order_0 / t_order_1
       -> ds1 -> t_order_0 / t_order_1
```

说明：`user_id` 决定落库，`order_id` 决定落表。企业里常用“用户维度分库 + 订单维度分表”的组合。

### 2) 读写分离：写主读从（叠加分表）

运行：

```bash
./mvnw -f sharding-jdbc-demo/pom.xml spring-boot:run -Dspring-boot.run.profiles=readwrite
```

配置见：`src/main/resources/application-readwrite.yml`

为了演示简单，`write_ds` 和 `read_ds_0` 指向同一个 H2 库，这样读写有一致数据，但你仍能在日志中看到 SQL 被路由到不同数据源。

```
INSERT/UPDATE -> write_ds
SELECT        -> read_ds_0
```

### 3) 扩容演示：2 库 -> 4 库

运行（这是“扩容后的目标配置”，不要直接切换生产规则）：

```bash
./mvnw -f sharding-jdbc-demo/pom.xml spring-boot:run -Dspring-boot.run.profiles=expand
```

配置见：`src/main/resources/application-expand.yml`

```
Before: ds0/ds1  (mod 2)
After : ds0/ds1/ds2/ds3 (mod 4)
```

扩容步骤（企业常见做法）：
1) 预先准备新库新表（本例 `schema-expand.sql`）。
2) 全量回填历史数据。
3) 双写或 CDC 同步增量。
4) 切换路由规则到新配置。
5) 对账验证后下线旧分片。

注意：扩容是“数据迁移 + 路由切换”的组合动作，不能只改规则。

## 可扩展方向

- 替换成 MySQL 并使用真实的雪花 id 生成器。
- 新增按时间分片或复合分片规则（例如 user_id + month）。
- 自定义分片算法（CLASS_BASED 或 SPI 扩展）。
