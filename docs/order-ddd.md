# 订单上下文 DDD 示例

> 目标：通过一个最小「下单 → 支付」切片，演示 DDD 的分层、聚合、不变式与领域事件。

## 业务场景与统一语言
- 上下文：订单（Order）负责管理顾客的购买请求。
- 关键概念：
  - `Order`（聚合根）：包含顾客、行项目、金额、状态。
  - `OrderItem`（值对象）：`skuId`、`quantity`、`unitPrice`，计算行小计。
  - `Money`（值对象）：金额，保留 2 位小数。
  - `OrderPlaced` / `OrderPaid`（领域事件）：下单/支付成功时产生的事实。
  - 状态：`CREATED` → `PAID`（示例中还预留 `CANCELED`）。

## 分层与代码位置
- 接口层（HTTP 适配）：`src/main/java/com/example/javalearn/order/interfaces/web/OrderController.java`
- 应用层（用例编排、事务）：`src/main/java/com/example/javalearn/order/application/OrderAppService.java`
- 领域层（模型与规则）：
  - 聚合与值对象：`order/domain/model/*.java`
  - 事件：`order/domain/event/*.java`
  - 仓储接口：`order/domain/repository/OrderRepository.java`
- 基础设施层（持久化实现+数据种子）：
  - 内存仓储：`order/infrastructure/persistence/InMemoryOrderRepository.java`
  - 种子数据：`order/infrastructure/config/OrderDataInitializer.java`

## 关键战术设计
- 聚合根 `Order` 的职责：
  - 校验至少有一条行项目，行项目数量必须为正。
  - 计算总价并保持不可变 `total`。
  - 控制状态流转：只允许 `CREATED` → `PAID`，防止重复支付；支付后不可取消（示例规则）。
  - 产出领域事件 `OrderPlaced`/`OrderPaid`，当前示例只存入 `pendingEvents` 并在应用层清理，方便挂接事件总线。
- 值对象不可变，保证计算的确定性（`Money`, `OrderItem`, `OrderId` 等）。
- 仓储以聚合为粒度（一次保存/加载整个订单），接口定义在领域层，实现放基础设施。

## 如何体验
先确保本地装有 Maven，启动应用：
```bash
mvn spring-boot:run
```

接口示例：
1) 查询种子订单：
```bash
curl http://localhost:8080/api/orders/order-100
```
2) 创建新订单（返回 orderId）：
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"c-9","items":[{"skuId":"sku-book","quantity":2,"unitPrice":12.30},{"skuId":"sku-pen","quantity":1,"unitPrice":1.20}]}'
```
3) 支付订单：
```bash
curl -X POST http://localhost:8080/api/orders/{orderId}/pay
```
超卖/重复支付等非法状态会返回 4xx/5xx，展示聚合不变式的保护作用。

## 可继续演进的方向
- 替换内存仓储为 JPA/数据库实现；为事件挂接消息总线。
- 增加优惠、库存校验等领域服务，将跨聚合一致性通过领域事件和补偿驱动。
- 拆分查询模型（CQRS），为列表/详情提供投影。 
