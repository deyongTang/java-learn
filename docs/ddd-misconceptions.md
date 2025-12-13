# DDD 最大的误解：PO/BO/VO 不是 DDD！

## 你遇到的"假 DDD"

### 典型的"伪 DDD"代码

```java
// ========== PO (Persistent Object) ==========
@Entity
@Table(name = "orders")
public class OrderPO {
    @Id
    private Long id;
    private String customerId;
    private BigDecimal totalAmount;
    private String status;
    // getter/setter
}

// ========== BO (Business Object) ==========
public class OrderBO {
    private Long id;
    private String customerId;
    private BigDecimal totalAmount;
    private String status;
    // getter/setter
}

// ========== VO (View Object) ==========
public class OrderVO {
    private Long id;
    private String customerId;
    private BigDecimal totalAmount;
    private String status;
    // getter/setter
}

// ========== Service ==========
@Service
public class OrderService {
    public OrderVO createOrder(OrderBO orderBO) {
        // 1. BO → PO
        OrderPO po = new OrderPO();
        po.setCustomerId(orderBO.getCustomerId());
        po.setTotalAmount(orderBO.getTotalAmount());
        po.setStatus("CREATED");
        
        // 2. 保存
        orderDao.save(po);
        
        // 3. PO → VO
        OrderVO vo = new OrderVO();
        vo.setId(po.getId());
        vo.setCustomerId(po.getCustomerId());
        vo.setTotalAmount(po.getTotalAmount());
        vo.setStatus(po.getStatus());
        
        return vo;
    }
    
    public void payOrder(Long orderId) {
        OrderPO po = orderDao.findById(orderId);
        
        // 业务逻辑在 Service 层
        if (!"CREATED".equals(po.getStatus())) {
            throw new RuntimeException("不能支付");
        }
        
        po.setStatus("PAID");
        orderDao.update(po);
    }
}
```

### 这种方式的问题

❌ **问题1：三个对象结构完全一样**
- PO、BO、VO 只是名字不同，字段完全一样
- 大量重复代码，互相转换

❌ **问题2：都是贫血模型**
- 所有对象都只有 getter/setter
- 没有业务行为

❌ **问题3：业务逻辑在 Service**
- Service 层承担所有业务逻辑
- 对象只是数据容器

❌ **问题4：不是业务语言**
- 代码中是 `setStatus("PAID")`
- 业务说的是"支付订单"

**这不是 DDD，这只是传统的三层架构 + 对象命名约定！**

---

## 真正的 DDD

### DDD 的核心不是 PO/BO/VO，而是：

1. **充血模型**：对象有行为，不只是数据
2. **业务语言**：代码说业务的话
3. **聚合**：保护业务不变式
4. **领域驱动**：业务逻辑在领域层

### 真正的 DDD 代码

```java
// ========== 值对象：Money ==========
public class Money {
    private final BigDecimal amount;
    
    public Money(BigDecimal amount) {
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("金额最多2位小数");
        }
        this.amount = amount;
    }
    
    // 有行为！
    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }
    
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }
}

// ========== 值对象：OrderItem ==========
public class OrderItem {
    private final String skuId;
    private final int quantity;
    private final Money unitPrice;
    
    public OrderItem(String skuId, int quantity, Money unitPrice) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("数量必须大于0");
        }
        this.skuId = skuId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }
    
    // 有行为！
    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }
}

// ========== 聚合根：Order ==========
public class Order {
    private final OrderId id;
    private final String customerId;
    private final List<OrderItem> items;
    private final Money total;
    private OrderStatus status;  // 枚举，不是字符串
    
    // 构造函数保护不变式
    public Order(OrderId id, String customerId, List<OrderItem> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("订单必须至少有一个商品");
        }
        this.id = id;
        this.customerId = customerId;
        this.items = new ArrayList<>(items);
        this.total = calculateTotal();  // 自动计算
        this.status = OrderStatus.CREATED;
    }
    
    // 业务方法，不是 setter！
    public void pay() {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("只有待支付订单才能支付");
        }
        this.status = OrderStatus.PAID;
    }
    
    public void cancel() {
        if (status == OrderStatus.PAID) {
            throw new IllegalStateException("已支付订单不能取消");
        }
        this.status = OrderStatus.CANCELED;
    }
    
    private Money calculateTotal() {
        return items.stream()
            .map(OrderItem::subtotal)
            .reduce(Money.ZERO, Money::add);
    }
    
    // 只有 getter，没有 setter
    public OrderId getId() { return id; }
    public Money getTotal() { return total; }
    public OrderStatus getStatus() { return status; }
}

// ========== 应用服务：只做编排 ==========
@Service
public class OrderAppService {
    private final OrderRepository orderRepository;
    
    @Transactional
    public String createOrder(CreateOrderCommand command) {
        // 构建值对象
        List<OrderItem> items = command.items().stream()
            .map(dto -> new OrderItem(
                dto.skuId(),
                dto.quantity(),
                new Money(dto.unitPrice())
            ))
            .collect(Collectors.toList());
        
        // 创建聚合（业务规则在构造函数中）
        Order order = new Order(
            OrderId.generate(),
            command.customerId(),
            items
        );
        
        // 保存
        orderRepository.save(order);
        
        return order.getId().getValue();
    }
    
    @Transactional
    public void payOrder(String orderId) {
        Order order = orderRepository.findById(new OrderId(orderId))
            .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        
        // 业务逻辑在聚合内部
        order.pay();
        
        orderRepository.save(order);
    }
}
```

---

## 对比表：伪 DDD vs 真 DDD

| 维度 | 伪 DDD (PO/BO/VO) | 真 DDD |
|------|------------------|--------|
| **对象类型** | PO、BO、VO | Entity、ValueObject、Aggregate |
| **对象特征** | 只有数据（getter/setter） | 有行为（业务方法） |
| **业务逻辑位置** | Service 层 | 领域对象内部 |
| **状态修改** | `setStatus("PAID")` | `order.pay()` |
| **对象转换** | PO↔BO↔VO 互相转换 | 领域对象 ↔ DTO |
| **类型安全** | 字符串状态 | 枚举/值对象 |
| **业务规则** | 散落在 Service | 集中在聚合 |
| **语言** | 技术语言 | 业务语言 |

---

## 为什么会有这种误解？

### 1. 历史原因

早期 Java 开发流行这种分层：

```
表现层 → 业务层 → 持久层
 VO       BO       PO
```

这是 **J2EE 时代的分层架构**，不是 DDD！

### 2. 培训机构误导

很多培训机构教的"DDD"：
- "PO 是数据库对象"
- "BO 是业务对象"
- "VO 是视图对象"
- "这就是 DDD 的分层"

**这是错的！**

### 3. 公司"伪实践"

很多公司说自己在用 DDD，但实际上：
- 只是改了对象命名（PO/BO/VO）
- 业务逻辑还是在 Service 层
- 对象还是贫血模型

**这不是 DDD，只是披着 DDD 外衣的传统架构！**

---

## DDD 真正关心的是什么？

### 不是对象命名，而是：

#### 1. 统一语言（Ubiquitous Language）

❌ **伪 DDD**
```java
order.setStatus("PAID");
```

✅ **真 DDD**
```java
order.pay();  // 说业务的话
```

#### 2. 充血模型（Rich Domain Model）

❌ **伪 DDD**
```java
public class Order {
    private String status;
    public void setStatus(String status) {
        this.status = status;
    }
}
```

✅ **真 DDD**
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
```

#### 3. 聚合（Aggregate）

❌ **伪 DDD**
```java
// 可以随意修改
order.getItems().add(new OrderItem());
order.setTotalAmount(xxx);  // 可能不一致
```

✅ **真 DDD**
```java
// 通过聚合根修改
order.addItem(item);  // 自动重新计算总价
```

#### 4. 业务逻辑在领域层

❌ **伪 DDD**
```java
// Service 层
if (order.getStatus().equals("CREATED")) {
    order.setStatus("PAID");
}
```

✅ **真 DDD**
```java
// 领域层
public void pay() {
    if (status != OrderStatus.CREATED) {
        throw new IllegalStateException("不能支付");
    }
    this.status = OrderStatus.PAID;
}

// Service 层
order.pay();
```

---

## 那 DDD 中有 PO/BO/VO 吗？

### DDD 的对象分类

DDD 不用 PO/BO/VO，而是：

#### 1. 领域层

- **Entity（实体）**：有唯一标识的对象
- **Value Object（值对象）**：无标识，不可变
- **Aggregate（聚合）**：一组对象的集合
- **Domain Service（领域服务）**：无状态的业务逻辑
- **Domain Event（领域事件）**：领域中发生的事实

#### 2. 应用层

- **Command（命令）**：表示用户意图
- **Query（查询）**：查询请求
- **DTO（数据传输对象）**：跨层传输数据

#### 3. 接口层

- **DTO（数据传输对象）**：API 请求/响应

#### 4. 基础设施层

- **PO（持久化对象）**：数据库映射（可选）

### 对象转换

```
HTTP 请求
    ↓
DTO (接口层)
    ↓
Command (应用层)
    ↓
Entity/ValueObject (领域层)
    ↓
PO (基础设施层，可选)
    ↓
数据库
```

**关键区别：**
- 伪 DDD：PO/BO/VO 结构完全一样，只是名字不同
- 真 DDD：每层对象有不同的职责和结构

---

## 实际例子对比

### 场景：订单支付

#### 伪 DDD（PO/BO/VO）

```java
// 1. Controller 接收 VO
@PostMapping("/{id}/pay")
public OrderVO payOrder(@PathVariable Long id) {
    return orderService.payOrder(id);
}

// 2. Service 处理 BO
public OrderVO payOrder(Long id) {
    // 查询 PO
    OrderPO po = orderDao.findById(id);
    
    // PO → BO
    OrderBO bo = new OrderBO();
    bo.setId(po.getId());
    bo.setStatus(po.getStatus());
    
    // 业务逻辑
    if (!"CREATED".equals(bo.getStatus())) {
        throw new RuntimeException("不能支付");
    }
    bo.setStatus("PAID");
    
    // BO → PO
    po.setStatus(bo.getStatus());
    orderDao.update(po);
    
    // PO → VO
    OrderVO vo = new OrderVO();
    vo.setId(po.getId());
    vo.setStatus(po.getStatus());
    return vo;
}
```

**问题：**
- PO/BO/VO 互相转换，大量重复代码
- 业务逻辑在 Service 层
- 对象只是数据容器

#### 真 DDD

```java
// 1. Controller 接收 DTO
@PostMapping("/{id}/pay")
public OrderResponse payOrder(@PathVariable String id) {
    orderAppService.payOrder(id);
    
    Order order = orderQueryService.getOrder(id);
    return OrderResponse.from(order);
}

// 2. 应用服务编排
@Transactional
public void payOrder(String orderId) {
    // 加载聚合
    Order order = orderRepository.findById(new OrderId(orderId))
        .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
    
    // 业务逻辑在聚合内部
    order.pay();
    
    // 保存聚合
    orderRepository.save(order);
}

// 3. 聚合内部
public class Order {
    public void pay() {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("只有待支付订单才能支付");
        }
        this.status = OrderStatus.PAID;
    }
}
```

**优势：**
- 业务逻辑在领域对象内部
- 应用服务只做编排
- 代码说业务的话

---

## 如何识别真假 DDD？

### 快速判断法

问自己 3 个问题：

#### 1. 对象有行为吗？

❌ **假 DDD**：只有 getter/setter
✅ **真 DDD**：有业务方法（`pay()`, `cancel()`, `enroll()`）

#### 2. 业务逻辑在哪里？

❌ **假 DDD**：在 Service 层
✅ **真 DDD**：在领域对象内部

#### 3. 代码说业务的话吗？

❌ **假 DDD**：`setStatus("PAID")`
✅ **真 DDD**：`order.pay()`

### 检查清单

- [ ] 领域对象有业务方法，不只是 getter/setter
- [ ] 业务逻辑在领域层，不在 Service 层
- [ ] 使用业务语言命名方法
- [ ] 值对象不可变
- [ ] 聚合保护不变式
- [ ] 没有大量的 PO/BO/VO 互相转换

---

## 总结

### 伪 DDD 的特征

```
PO/BO/VO + 贫血模型 + Service 层业务逻辑
= 传统三层架构 + 对象命名规范
≠ DDD
```

### 真 DDD 的特征

```
Entity/ValueObject/Aggregate + 充血模型 + 领域层业务逻辑
= 领域驱动设计
```

### 记住

**DDD 的核心不是对象命名（PO/BO/VO），而是：**

1. **让代码说业务的语言**
2. **让对象有行为，不只是数据**
3. **让业务逻辑在领域层，不在 Service 层**
4. **用聚合保护业务不变式**

**如果你的代码只是改了对象名字（PO/BO/VO），但业务逻辑还在 Service 层，对象还是贫血模型，那不是 DDD！**

---

## 延伸阅读

- 查看 `ddd-learning-guide.md` 了解真正的 DDD
- 查看 `ddd-vs-traditional.md` 看完整的代码对比
- 查看 `ddd-hands-on-tutorial.md` 动手实践真正的 DDD
