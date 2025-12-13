# DDD 快速参考指南

## 一、核心概念速查

### 战略设计（宏观）

| 概念 | 定义 | 何时使用 |
|------|------|---------|
| **限界上下文** | 业务边界，内部有统一的模型 | 划分大型系统，避免大泥球 |
| **统一语言** | 业务和技术共同的术语 | 所有沟通和代码 |
| **上下文映射** | 不同上下文之间的关系 | 多团队协作 |

### 战术设计（微观）

| 概念 | 特征 | 示例 | 何时使用 |
|------|------|------|---------|
| **实体** | 有唯一标识，可变 | `Order`, `User` | 需要跟踪生命周期 |
| **值对象** | 无标识，不可变，通过值相等 | `Money`, `Email`, `Address` | 描述性概念，可替换 |
| **聚合** | 一组对象的集合，有聚合根 | `Order` + `OrderItem` | 保护业务不变式 |
| **领域服务** | 无状态的业务逻辑 | `TransferService` | 逻辑不属于任何实体 |
| **领域事件** | 领域中发生的事实 | `OrderPaid`, `UserRegistered` | 解耦聚合，异步处理 |
| **仓储** | 聚合的持久化接口 | `OrderRepository` | 封装数据访问 |

---

## 二、实体 vs 值对象

### 判断标准

```
问：两个对象的属性完全相同，它们是同一个对象吗？

是 → 值对象
否 → 实体
```

### 示例

```java
// 实体：两个用户即使名字相同，也是不同的人
User user1 = new User(UserId("001"), "张三");
User user2 = new User(UserId("002"), "张三");
user1.equals(user2);  // false（ID 不同）

// 值对象：两个金额如果数值相同，就是同一个金额
Money money1 = new Money("100.00");
Money money2 = new Money("100.00");
money1.equals(money2);  // true（值相同）
```

### 设计检查清单

**实体：**
- ✅ 有唯一标识（ID）
- ✅ 可以修改属性
- ✅ 通过 ID 判断相等
- ✅ 有生命周期

**值对象：**
- ✅ 无标识
- ✅ 不可变（immutable）
- ✅ 通过属性判断相等
- ✅ 可以随时替换

---

## 三、聚合设计原则

### 1. 聚合要尽量小

❌ **错误：大聚合**
```
Order 聚合包含：
- Order
- OrderItem
- Customer
- Product
- Inventory
```

✅ **正确：小聚合**
```
Order 聚合只包含：
- Order（聚合根）
- OrderItem（聚合内部）

Customer、Product、Inventory 是独立聚合
```

### 2. 聚合之间通过 ID 引用

❌ **错误：直接引用**
```java
public class Order {
    private Customer customer;  // 直接持有对象
}
```

✅ **正确：ID 引用**
```java
public class Order {
    private CustomerId customerId;  // 只持有 ID
}
```

### 3. 一个事务只修改一个聚合

❌ **错误：跨聚合事务**
```java
@Transactional
public void placeOrder(OrderId orderId, ProductId productId) {
    Order order = orderRepo.find(orderId);
    Product product = productRepo.find(productId);
    
    order.addItem(product);  // 修改 Order
    product.decreaseStock(); // 修改 Product
    
    orderRepo.save(order);
    productRepo.save(product);
}
```

✅ **正确：通过事件最终一致**
```java
@Transactional
public void placeOrder(OrderId orderId, ProductId productId) {
    Order order = orderRepo.find(orderId);
    order.addItem(productId);  // 只修改 Order
    orderRepo.save(order);
    
    // 发布事件
    eventBus.publish(new OrderPlaced(orderId, productId));
}

// 另一个事务处理库存
@EventListener
public void onOrderPlaced(OrderPlaced event) {
    Product product = productRepo.find(event.getProductId());
    product.decreaseStock();
    productRepo.save(product);
}
```

### 4. 聚合根保护不变式

```java
public class Order {
    private List<OrderItem> items;
    private Money total;
    
    // ❌ 错误：暴露内部集合
    public List<OrderItem> getItems() {
        return items;  // 外部可以直接修改
    }
    
    // ✅ 正确：通过方法修改
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
    
    public void addItem(OrderItem item) {
        items.add(item);
        this.total = calculateTotal();  // 保持一致性
    }
}
```

---

## 四、分层架构速查

```
┌─────────────────────────────────────────┐
│  接口层 (Interfaces)                     │
│  - Controller: 处理 HTTP 请求            │
│  - DTO: 数据传输对象                     │
│  - Assembler: DTO ↔ 领域对象转换         │
├─────────────────────────────────────────┤
│  应用层 (Application)                    │
│  - AppService: 用例编排                  │
│  - Command: 命令对象                     │
│  - Query: 查询对象                       │
│  - 事务管理                              │
├─────────────────────────────────────────┤
│  领域层 (Domain)                         │
│  - Entity: 实体                          │
│  - ValueObject: 值对象                   │
│  - Aggregate: 聚合                       │
│  - DomainService: 领域服务               │
│  - DomainEvent: 领域事件                 │
│  - Repository 接口                       │
├─────────────────────────────────────────┤
│  基础设施层 (Infrastructure)             │
│  - Repository 实现                       │
│  - 数据库访问                            │
│  - 消息队列                              │
│  - 缓存                                  │
└─────────────────────────────────────────┘
```

### 各层职责

| 层 | 职责 | 不应该做 |
|---|------|---------|
| **接口层** | 接收请求，返回响应 | 业务逻辑 |
| **应用层** | 编排用例，管理事务 | 业务规则 |
| **领域层** | 业务逻辑，业务规则 | 依赖框架 |
| **基础设施层** | 技术实现 | 业务逻辑 |

### 依赖规则

```
接口层 → 应用层 → 领域层 ← 基础设施层
                    ↑
                  核心
```

- 上层可以依赖下层
- 领域层不依赖任何层
- 基础设施层依赖领域层（实现接口）

---

## 五、常见模式速查

### 1. 工厂模式

**何时使用：** 创建复杂对象

```java
public class OrderFactory {
    public Order createOrder(String customerId, List<OrderItemDto> items) {
        List<OrderItem> orderItems = items.stream()
            .map(dto -> new OrderItem(dto.skuId, dto.quantity, new Money(dto.price)))
            .collect(Collectors.toList());
        
        return new Order(OrderId.generate(), customerId, orderItems);
    }
}
```

### 2. 仓储模式

**何时使用：** 封装聚合的持久化

```java
// 接口在领域层
public interface OrderRepository {
    Optional<Order> findById(OrderId id);
    void save(Order order);
    void delete(OrderId id);
}

// 实现在基础设施层
public class JpaOrderRepository implements OrderRepository {
    @Override
    public void save(Order order) {
        // JPA 实现
    }
}
```

### 3. 领域事件模式

**何时使用：** 解耦聚合，异步处理

```java
// 1. 定义事件
public class OrderPaid {
    private final OrderId orderId;
    private final Instant occurredAt;
}

// 2. 聚合产生事件
public class Order {
    public void pay() {
        this.status = OrderStatus.PAID;
        addEvent(new OrderPaid(this.id));
    }
}

// 3. 应用服务发布事件
@Transactional
public void payOrder(OrderId orderId) {
    Order order = orderRepo.find(orderId);
    order.pay();
    orderRepo.save(order);
    
    eventBus.publish(order.getPendingEvents());
}

// 4. 其他上下文监听事件
@EventListener
public void onOrderPaid(OrderPaid event) {
    // 扣减库存
    // 发送通知
}
```

### 4. 规格模式（Specification）

**何时使用：** 复杂查询条件

```java
public interface Specification<T> {
    boolean isSatisfiedBy(T candidate);
}

public class OverdueOrderSpec implements Specification<Order> {
    @Override
    public boolean isSatisfiedBy(Order order) {
        return order.isOverdue();
    }
}

// 使用
List<Order> overdueOrders = orders.stream()
    .filter(new OverdueOrderSpec()::isSatisfiedBy)
    .collect(Collectors.toList());
```

---

## 六、代码检查清单

### 领域层检查

- [ ] 实体有唯一标识
- [ ] 值对象不可变
- [ ] 聚合根保护不变式
- [ ] 没有 setter，只有业务方法
- [ ] 方法名使用业务语言
- [ ] 不依赖框架（Spring、JPA 等）
- [ ] 可以独立测试

### 应用层检查

- [ ] 只做编排，不包含业务逻辑
- [ ] 管理事务边界
- [ ] 一个事务只修改一个聚合
- [ ] 发布领域事件

### 接口层检查

- [ ] 只处理 HTTP 相关逻辑
- [ ] DTO 和领域对象分离
- [ ] 不包含业务逻辑

---

## 七、常见错误

### 错误1：贫血模型

❌ **错误**
```java
public class Order {
    private String status;
    public void setStatus(String status) {
        this.status = status;
    }
}

// Service 层
order.setStatus("PAID");
```

✅ **正确**
```java
public class Order {
    private OrderStatus status;
    public void pay() {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("不能支付");
        }
        this.status = OrderStatus.PAID;
    }
}

// Service 层
order.pay();
```

### 错误2：聚合过大

❌ **错误**
```java
public class Order {
    private Customer customer;      // 应该是独立聚合
    private List<Product> products; // 应该是独立聚合
    private Shipping shipping;      // 应该是独立聚合
}
```

✅ **正确**
```java
public class Order {
    private CustomerId customerId;  // 只持有 ID
    private List<OrderItem> items;  // 聚合内部对象
}
```

### 错误3：领域层依赖框架

❌ **错误**
```java
@Entity  // JPA 注解
public class Order {
    @Id
    private Long id;
}
```

✅ **正确**
```java
// 领域层：纯 POJO
public class Order {
    private OrderId id;
}

// 基础设施层：JPA 实体
@Entity
public class OrderJpaEntity {
    @Id
    private String id;
    
    public Order toDomain() {
        // 转换为领域对象
    }
}
```

### 错误4：跨聚合事务

❌ **错误**
```java
@Transactional
public void transfer(AccountId from, AccountId to, Money amount) {
    Account fromAccount = accountRepo.find(from);
    Account toAccount = accountRepo.find(to);
    
    fromAccount.debit(amount);
    toAccount.credit(amount);
    
    accountRepo.save(fromAccount);
    accountRepo.save(toAccount);  // 修改两个聚合
}
```

✅ **正确**
```java
// 方案1：使用领域服务（如果必须强一致）
@Transactional
public void transfer(AccountId from, AccountId to, Money amount) {
    Account fromAccount = accountRepo.find(from);
    Account toAccount = accountRepo.find(to);
    
    TransferService.transfer(fromAccount, toAccount, amount);
    
    accountRepo.save(fromAccount);
    accountRepo.save(toAccount);
}

// 方案2：使用事件（最终一致）
@Transactional
public void debit(AccountId from, Money amount) {
    Account account = accountRepo.find(from);
    account.debit(amount);
    accountRepo.save(account);
    
    eventBus.publish(new MoneyDebited(from, amount));
}

@EventListener
public void onMoneyDebited(MoneyDebited event) {
    Account account = accountRepo.find(event.getToAccount());
    account.credit(event.getAmount());
    accountRepo.save(account);
}
```

---

## 八、测试策略

### 领域层测试（单元测试）

```java
@Test
public void should_pay_order_when_status_is_created() {
    // Given
    Order order = new Order(
        OrderId.generate(),
        "customer-1",
        List.of(new OrderItem("sku-1", 2, new Money("10.00")))
    );
    
    // When
    order.pay();
    
    // Then
    assertEquals(OrderStatus.PAID, order.getStatus());
}

@Test
public void should_throw_exception_when_pay_paid_order() {
    // Given
    Order order = new Order(...);
    order.pay();
    
    // When & Then
    assertThrows(IllegalStateException.class, () -> order.pay());
}
```

### 应用层测试（集成测试）

```java
@SpringBootTest
@Transactional
public class OrderAppServiceTest {
    
    @Autowired
    private OrderAppService orderAppService;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Test
    public void should_create_order() {
        // Given
        CreateOrderCommand command = new CreateOrderCommand(...);
        
        // When
        String orderId = orderAppService.createOrder(command);
        
        // Then
        Order order = orderRepository.findById(new OrderId(orderId)).get();
        assertEquals(OrderStatus.CREATED, order.getStatus());
    }
}
```

---

## 九、重构步骤

### 从传统代码重构到 DDD

**步骤1：识别聚合**
- 找出哪些对象需要一起修改
- 确定聚合根

**步骤2：提取值对象**
- 找出可以用值对象表示的概念
- 封装业务规则

**步骤3：移动业务逻辑**
- 把 Service 层的业务逻辑移到聚合内部
- Service 只保留编排逻辑

**步骤4：引入领域事件**
- 识别重要的业务事实
- 用事件解耦聚合

**步骤5：重构仓储**
- 以聚合为单位保存和加载
- 接口定义在领域层

---

## 十、推荐阅读顺序

1. **入门**：《领域驱动设计精粹》（Vaughn Vernon）
2. **实战**：《实现领域驱动设计》（Vaughn Vernon）
3. **深入**：《领域驱动设计》（Eric Evans）
4. **模式**：《企业应用架构模式》（Martin Fowler）

---

## 十一、快速决策树

```
需要 DDD 吗？
├─ 业务简单（CRUD）？
│  └─ 否 → 不需要 DDD
├─ 业务复杂？
│  ├─ 是 → 需要 DDD
│  └─ 否 → 不需要 DDD
├─ 需要长期维护？
│  ├─ 是 → 考虑 DDD
│  └─ 否 → 不需要 DDD
└─ 团队熟悉 DDD？
   ├─ 是 → 可以使用 DDD
   └─ 否 → 先学习再决定
```

---

## 十二、术语对照表

| 中文 | 英文 | 缩写 |
|------|------|------|
| 领域驱动设计 | Domain-Driven Design | DDD |
| 限界上下文 | Bounded Context | BC |
| 统一语言 | Ubiquitous Language | UL |
| 聚合 | Aggregate | - |
| 聚合根 | Aggregate Root | AR |
| 实体 | Entity | - |
| 值对象 | Value Object | VO |
| 领域服务 | Domain Service | - |
| 领域事件 | Domain Event | - |
| 仓储 | Repository | - |
| 工厂 | Factory | - |
| 应用服务 | Application Service | - |
| 命令查询分离 | Command Query Responsibility Segregation | CQRS |
| 事件溯源 | Event Sourcing | ES |

---

**记住：DDD 的核心是让代码说业务的语言！**
