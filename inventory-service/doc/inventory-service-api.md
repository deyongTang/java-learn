# Inventory Service API 文档

本文档描述 `inventory-service` 对外提供的 HTTP 接口，代码入口为 `inventory-service/src/main/java/com/example/txdemo/inventory/web/InventoryController.java`。

## 基本信息

- 服务：`inventory-service`
- 默认端口：`8081`（见 `inventory-service/src/main/resources/application.yml`）
- Base URL：`http://localhost:8081`
- Content-Type：请求体为 JSON 的接口请使用 `application/json`

## 数据模型

### SeedRequest

用于初始化/重置某个商品的可用库存。

```json
{
  "productId": "product-1",
  "available": 5
}
```

字段：
- `productId`（string，必填）：商品 ID
- `available`（int，必填）：可用库存数量（建议为非负数）

### ReserveRequest

用于预留库存（扣减 available，增加 reserved）。

```json
{
  "productId": "product-1",
  "quantity": 2
}
```

字段：
- `productId`（string，必填）：商品 ID
- `quantity`（int，必填）：预留数量（建议为正整数）

### ReleaseRequest

用于释放库存（增加 available，扣减 reserved）。

```json
{
  "productId": "product-1",
  "quantity": 2
}
```

字段：
- `productId`（string，必填）：商品 ID
- `quantity`（int，必填）：释放数量（建议为正整数）

### InventoryView（查询返回）

```json
{
  "productId": "product-1",
  "available": 3,
  "reserved": 2
}
```

字段：
- `productId`（string）
- `available`（int）
- `reserved`（int）

## 接口列表

### 1) 初始化库存

- Method：`POST`
- Path：`/inventory/seed`
- 描述：初始化/更新某个 `productId` 的可用库存；用于联调前准备数据。
- Request Body：`SeedRequest`
- Response：
  - `200 OK`：无响应体

示例：
```bash
curl -X POST http://localhost:8081/inventory/seed \
  -H 'Content-Type: application/json' \
  -d '{"productId":"product-1","available":5}'
```

行为说明：
- 底层是 upsert 语义：重复调用会覆盖同一 `productId` 的 `available`（reserved 初始化为 0）。

### 2) 预留库存

- Method：`POST`
- Path：`/inventory/reserve`
- 描述：对指定商品预留库存；内部会对同一 `productId` 使用 Redis 分布式锁避免并发超卖。
- Request Body：`ReserveRequest`
- Response：
  - `200 OK`：预留成功，无响应体
  - `409 Conflict`：预留失败（库存不足/商品不存在/获取锁失败等），响应体为错误信息字符串

示例：
```bash
curl -X POST http://localhost:8081/inventory/reserve \
  -H 'Content-Type: application/json' \
  -d '{"productId":"product-1","quantity":2}'
```

常见失败响应示例（HTTP 409）：
- `库存不足或商品不存在: product-1`
- `获取分布式锁失败: product-1`

### 3) 释放库存

- Method：`POST`
- Path：`/inventory/release`
- 描述：释放已预留的库存（available 增加、reserved 减少）。
- Request Body：`ReleaseRequest`
- Response：
  - `200 OK`：无响应体

示例：
```bash
curl -X POST http://localhost:8081/inventory/release \
  -H 'Content-Type: application/json' \
  -d '{"productId":"product-1","quantity":2}'
```

说明：
- 该 Demo 未对 `reserved` 下限做强校验（如果重复释放，可能出现 reserved 为负数的异常数据）；生产场景建议增加约束与幂等控制。

### 4) 查询库存

- Method：`GET`
- Path：`/inventory/{productId}`
- 描述：查询某个商品库存视图（available/reserved）。
- Path Param：
  - `productId`：商品 ID
- Response：
  - `200 OK`：返回 `InventoryView`
  - `404 Not Found`：商品不存在

示例：
```bash
curl http://localhost:8081/inventory/product-1
```

## 与分布式事务（Saga）关系

这些 HTTP 接口主要用于“人工联调/演示”；在 Saga 主流程中，库存预留/失败结果通常由 RocketMQ 事件驱动触发（详见 `docs/distributed-transaction-demo.md`）。

