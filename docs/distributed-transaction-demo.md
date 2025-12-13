# 微服务下的分布式事务（Saga）实践 Demo

这个 Demo 通过**订单服务**和**库存服务**展示了 Saga 协调式的分布式事务，突出“**失败后补偿**”的关键思路。

## 架构思路
- **每个服务只做本地事务**：订单服务负责创建/取消订单，库存服务负责预留/释放库存。
- **Saga 协调器串联步骤**：编排执行顺序（预留库存 → 标记订单），任何一步失败即触发已完成步骤的补偿逻辑。
- **幂等 & 可补偿**：每个步骤都有对应的补偿操作，模拟真实系统中“取消订单”“释放库存”的接口。

## 代码入口
- `com.example.distributedtx.DemoApplication`：演示成功和失败两条链路。
- `com.example.distributedtx.saga.OrderInventorySaga`：Saga 协调器，负责执行与补偿。
- `com.example.distributedtx.service.*` & `com.example.distributedtx.model.*`：模拟两个微服务的本地事务。

运行方式（需要 JDK 17+ 与 Maven）：
```bash
mvn -q exec:java -Dexec.mainClass="com.example.distributedtx.DemoApplication"
```

## 关键片段说明
### Saga 步骤定义
```java
new SagaStep("预留库存", () -> {
    if (simulateInventoryFailure) {
        throw new IllegalStateException("刻意制造的库存预留失败");
    }
    inventoryService.reserve(productId, quantity);
}, () -> inventoryService.release(productId, quantity));
```
- `action`：正向执行预留库存。
- `compensation`：失败时释放库存。

### 执行与补偿
```java
try {
    for (SagaStep step : steps) {
        step.execute();
        executed.push(step);
    }
    // 全部成功 => 提交
} catch (Exception ex) {
    while (!executed.isEmpty()) {
        executed.pop().compensate();
    }
    // 补偿完成 => 回滚
}
```

## Demo 输出解读
1. **成功案例**：预留库存、订单进入 RESERVED，Saga 提交。
2. **失败案例**：故意制造库存失败 → 触发“释放库存 + 取消订单”补偿，Saga 回滚。

通过简单的可运行代码，可以观察 Saga 模式如何在微服务场景下用补偿动作保证数据一致性。
