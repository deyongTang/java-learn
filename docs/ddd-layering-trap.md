# DDD 最大的陷阱：分层 ≠ DDD

## 你的困惑

> "我之前做的项目一直都是分层：基础层、Service 层、应用层、领域层"

**这是最常见的误解：以为分了四层就是 DDD！**

---

## 问题诊断：你的项目是真 DDD 吗？

### 测试 1：看你的"领域层"代码

#### 场景 A：假 DDD（只是分层）

```
项目结构：
├── domain/          ← 有"领域层"
│   ├── Order.java
│   └── OrderItem.java
├── service/         ← 有"Service 层"
│   └── OrderService.java
├── application/     ← 有"应用层"
│   └── OrderAppService.java
└── infrastructure/  ← 有"基础设施层"
    └── OrderDao.java
```

**看起来很 DDD 对吧？但是：**

```java
// domain/Order.java（所谓的"领域层"）
package com.example.domain;

public class Order {
    private Long id;
    private String customerId;
    private BigDecimal totalAmount;
    private String status;
    
    // 只有 getter/setter，没有业务逻辑！
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { 
        this.totalAmount = totalAmount; 
    }
    // ... 其他 getter/setter
}

// service/OrderService.java
package com.example.service;

@Service
public class OrderService {
    
    @Autowired
    private OrderDao orderDao;
    
    // 所有业务逻辑在这里！
    public void payOrder(Long orderId) {
        Order order = orderDao.findById(orderId);
        
        // 业务规则在 Service 层
        if (!"CREATED".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不允许支付");
        }
        
        // 直接修改状态
        order.setStatus("PAID");
        
        // 可能还要手动计算总价
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            total = total.add(
                item.getUnitPrice().multiply(
                    BigDecimal.valueOf(item.getQuantity())
                )
            );
        }
        order.setTotalAmount(total);
        
        orderDao.update(order);
    }
}

// application/OrderAppService.java
package com.example.application;

@Service
public class OrderAppService {
    
    @Autowired
    private OrderService orderService;
    
    // 只是转发调用
    public void payOrder(Long orderId) {
        orderService.payOrder(orderId);
    }
}
```

**这不是 DDD！这只是：**
- ❌ 有分层，但领域层是贫血模型
- ❌ 业务逻辑在 Service 层
- ❌ 应用层和 Service 层职责不清
- ❌ 对象只是数据容器

---

#### 场景 B：真 DDD

```
项目结构：
├── domain/
│   ├── model/
│   │   ├── Order.java        ← 聚合根（有行为）
│   │   ├── OrderItem.java    ← 值对象
│   │   ├── Money.java        ← 值对象
│   │   └── OrderStatus.java  ← 枚举
│   ├── event/
│   │   └── OrderPaid.java
│   └── repository/
│       └── OrderRepository.java  ← 接口
├── application/
│   └── OrderAppService.java      ← 用例编排
├── infrastructure/
│   └── persistence/
│       └── OrderRepositoryImpl.java
└── interfaces/
    └── OrderController.java
```

**关键区别：**

```java
// domain/model/Order.java（真正的领域层）
package com.example.domain.model;

public class Order {
    private final OrderId id;
    private final String customerId;
    private final List<OrderItem> items;
    private final Money total;  // 不可变
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
    
    // 业务逻辑在领域对象内部
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

// application/OrderAppService.java（真正的应用层）
package com.example.application;

@Service
public class OrderAppService {
    
    private final OrderRepository orderRepository;
    
    // 只做编排，不包含业务逻辑
    @Transactional
    public void payOrder(String orderId) {
        Order order = orderRepository.findById(new OrderId(orderId))
            .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        
        // 业务逻辑在 Order 内部
        order.pay();
        
        orderRepository.save(order);
    }
}
```

**这才是 DDD：**
- ✅ 领域层有业务逻辑
- ✅ 应用层只做编排
- ✅ 对象有行为
- ✅ 没有 Service 层（或者 Service 就是应用层）

---

## 核心问题：你的项目有几个 Service？

### 问题场景：Service 层和应用层都有

```
├── service/
│   └── OrderService.java      ← 这是什么？
├── application/
│   └── OrderAppService.java   ← 这又是什么？
```

**常见的混乱：**

```java
// OrderService.java
@Service
public class OrderService {
    public void payOrder(Long orderId) {
        // 业务逻辑在这里
        Order order = orderDao.findById(orderId);
        if (!"CREATED".equals(order.getStatus())) {
            throw new RuntimeException("不能支付");
        }
        order.setStatus("PAID");
        orderDao.update(order);
    }
}

// OrderAppService.java
@Service
public class OrderAppService {
    @Autowired
    private OrderService orderService;
    
    // 只是转发？
    public void payOrder(Long orderId) {
        orderService.payOrder(orderId);
    }
}
```

**问题：**
- Service 和 AppService 职责不清
- 业务逻辑在 Service 层
- AppService 只是转发，没有意义

---

## 对比表：假 DDD vs 真 DDD

| 维度 | 假 DDD（只是分层） | 真 DDD |
|------|------------------|--------|
| **分层** | ✅ 有四层 | ✅ 有四层 |
| **领域层对象** | 只有 getter/setter | 有业务方法 |
| **业务逻辑位置** | Service 层 | 领域对象内部 |
| **应用层职责** | 转发调用 | 编排用例、管理事务 |
| **Service 层** | 有，承担业务逻辑 | 没有（或就是应用层） |
| **状态修改** | `setStatus("PAID")` | `order.pay()` |
| **值对象** | 没有 | 有（Money, OrderItem） |
| **聚合** | 没有概念 | 有，保护不变式 |
| **领域事件** | 没有 | 有 |

---

## 你的项目可能是这样的

### 典型的"假 DDD"项目结构

```
src/main/java/com/example/
├── domain/                    ← 名字叫"领域层"
│   ├── Order.java            ← 但只有 getter/setter
│   └── OrderItem.java        ← 贫血模型
│
├── service/                   ← Service 层
│   └── OrderService.java     ← 所有业务逻辑在这里
│
├── application/               ← 应用层
│   └── OrderAppService.java  ← 只是转发调用
│
├── infrastructure/            ← 基础设施层
│   └── OrderDao.java
│
└── controller/
    └── OrderController.java
```

**调用链：**
```
Controller → AppService → Service → Dao
                            ↑
                      业务逻辑在这里
```

**问题：**
- 虽然分了四层，但本质还是传统三层架构
- 领域层是贫血模型
- Service 层承担业务逻辑
- AppService 没有实际作用

---

## 真正的 DDD 项目结构

```
src/main/java/com/example/
├── domain/                           ← 领域层（核心）
│   ├── model/
│   │   ├── Order.java               ← 聚合根（有行为）
│   │   ├── OrderId.java             ← 值对象
│   │   ├── OrderItem.java           ← 值对象
│   │   ├── Money.java               ← 值对象
│   │   └── OrderStatus.java         ← 枚举
│   ├── event/
│   │   └── OrderPaid.java           ← 领域事件
│   ├── repository/
│   │   └── OrderRepository.java     ← 仓储接口
│   └── service/
│       └── PricingService.java      ← 领域服务（可选）
│
├── application/                      ← 应用层
│   ├── OrderAppService.java         ← 用例编排
│   ├── command/
│   │   └── CreateOrderCommand.java  ← 命令
│   └── query/
│       └── OrderQueryService.java   ← 查询
│
├── infrastructure/                   ← 基础设施层
│   ├── persistence/
│   │   ├── OrderRepositoryImpl.java ← 仓储实现
│   │   └── OrderJpaEntity.java      ← JPA 实体
│   └── config/
│       └── DataInitializer.java
│
└── interfaces/                       ← 接口层
    └── web/
        ├── OrderController.java      ← HTTP 接口
        └── dto/
            └── OrderResponse.java    ← DTO
```

**调用链：**
```
Controller → AppService → Domain Model
                              ↑
                        业务逻辑在这里
```

**关键：**
- 没有单独的 Service 层（或者领域服务在 domain/service）
- 业务逻辑在领域对象内部
- 应用层做编排和事务管理

---

## 快速诊断：你的项目是哪种？

### 问题 1：你的领域层对象长什么样？

**A. 这样的（假 DDD）：**
```java
public class Order {
    private Long id;
    private String status;
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
```

**B. 这样的（真 DDD）：**
```java
public class Order {
    private OrderStatus status;
    
    public void pay() {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("不能支付");
        }
        this.status = OrderStatus.PAID;
    }
    
    // 没有 setStatus
}
```

### 问题 2：你的业务逻辑在哪里？

**A. 在 Service 层（假 DDD）：**
```java
@Service
public class OrderService {
    public void payOrder(Long orderId) {
        Order order = orderDao.findById(orderId);
        if (!"CREATED".equals(order.getStatus())) {  // 业务逻辑
            throw new RuntimeException("不能支付");
        }
        order.setStatus("PAID");
        orderDao.update(order);
    }
}
```

**B. 在领域对象内部（真 DDD）：**
```java
public class Order {
    public void pay() {  // 业务逻辑在这里
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("不能支付");
        }
        this.status = OrderStatus.PAID;
    }
}

@Service
public class OrderAppService {
    @Transactional
    public void payOrder(String orderId) {
        Order order = orderRepository.findById(orderId);
        order.pay();  // 只是调用
        orderRepository.save(order);
    }
}
```

### 问题 3：你有 Service 层和应用层吗？

**A. 两个都有（假 DDD）：**
```
service/OrderService.java      ← 业务逻辑
application/OrderAppService.java  ← 转发调用
```

**B. 只有应用层（真 DDD）：**
```
application/OrderAppService.java  ← 编排用例
domain/service/PricingService.java  ← 领域服务（可选）
```

---

## 为什么会这样？

### 原因 1：误解了 DDD 的分层

很多人以为：
```
DDD = 四层架构
```

实际上：
```
DDD = 领域驱动 + 充血模型 + 业务语言
分层只是实现方式之一
```

### 原因 2：从传统架构"升级"

```
传统三层：
Controller → Service → Dao

"升级"到 DDD：
Controller → AppService → Service → Dao
                           ↑
                     加了一层，但本质没变
```

### 原因 3：不敢动 Service 层

```
程序员想法：
"我们要用 DDD，但 Service 层的代码太多了，不敢动"
"那就加一个 AppService 层吧"
"再把实体类放到 domain 包"
"这样就是 DDD 了！"
```

**结果：只是改了包名，本质没变**

---

## 如何判断你的项目？

### 3 步快速判断

#### 步骤 1：打开你的"领域层"代码

```java
// 找到你的 Order.java 或类似的类
public class Order {
    // 看这里
}
```

**问自己：**
- 有 `setStatus()` 这样的 setter 吗？
- 有 `pay()` 这样的业务方法吗？

**如果只有 setter，没有业务方法 → 假 DDD**

#### 步骤 2：找到支付订单的代码

**问自己：**
- 状态检查逻辑在哪里？
- 是在 Service 层还是在 Order 内部？

**如果在 Service 层 → 假 DDD**

#### 步骤 3：看项目结构

**问自己：**
- 有 Service 层和应用层两个吗？
- 它们的职责是什么？

**如果 Service 层有业务逻辑，AppService 只是转发 → 假 DDD**

---

## 重构建议

### 如果你的项目是"假 DDD"

#### 步骤 1：识别业务逻辑

找出 Service 层中的业务规则：
```java
// OrderService.java
if (!"CREATED".equals(order.getStatus())) {  // ← 这是业务规则
    throw new RuntimeException("不能支付");
}
order.setStatus("PAID");  // ← 这是业务操作
```

#### 步骤 2：移动到领域对象

```java
// Order.java
public void pay() {
    if (status != OrderStatus.CREATED) {
        throw new IllegalStateException("只有待支付订单才能支付");
    }
    this.status = OrderStatus.PAID;
}
```

#### 步骤 3：简化 Service 层

```java
// OrderAppService.java（重命名或合并）
@Transactional
public void payOrder(String orderId) {
    Order order = orderRepository.findById(orderId);
    order.pay();  // 业务逻辑在这里
    orderRepository.save(order);
}
```

#### 步骤 4：提取值对象

```java
// 之前
private BigDecimal totalAmount;

// 之后
private Money total;

// Money.java
public class Money {
    private final BigDecimal amount;
    
    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }
}
```

---

## 总结

### 分层 ≠ DDD

```
有四层 + 贫血模型 + Service 层业务逻辑
= 假 DDD（只是分层）

有四层 + 充血模型 + 领域层业务逻辑
= 真 DDD
```

### 关键不是分层，而是：

1. **业务逻辑在哪里？**
   - 假 DDD：在 Service 层
   - 真 DDD：在领域对象内部

2. **对象有行为吗？**
   - 假 DDD：只有 getter/setter
   - 真 DDD：有业务方法

3. **代码说业务的话吗？**
   - 假 DDD：`setStatus("PAID")`
   - 真 DDD：`order.pay()`

### 记住

**DDD 的核心是领域驱动，不是分层！**

分层只是组织代码的方式，关键是：
- 让业务逻辑在领域层
- 让对象有行为
- 让代码说业务的语言

**如果你的项目只是分了四层，但领域层是贫血模型，业务逻辑在 Service 层，那不是 DDD！**

---

## 下一步

1. 检查你的项目，看是真 DDD 还是假 DDD
2. 如果是假 DDD，按照重构步骤逐步改进
3. 参考项目中的 `order` 和 `course` 示例
4. 阅读 `ddd-hands-on-tutorial.md` 学习真正的 DDD