# 一张图看清：真 DDD vs 假 DDD

## 场景：订单支付

---

## 假 DDD（你接触到的 PO/BO/VO）

### 代码结构

```
OrderController.java
OrderService.java
OrderDao.java
OrderPO.java    ← 数据库对象
OrderBO.java    ← 业务对象（其实和 PO 一样）
OrderVO.java    ← 视图对象（其实和 PO 一样）
```

### 完整代码

```java
// ==================== OrderPO ====================
@Entity
@Table(name = "orders")
public class OrderPO {
    @Id
    private Long id;
    private String customerId;
    private BigDecimal totalAmount;
    private String status;  // "CREATED", "PAID"
    
    // 只有 getter/setter，没有业务逻辑
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    // ... 其他 getter/setter
}

// ==================== OrderBO ====================
public class OrderBO {
    private Long id;
    private String customerId;
    private BigDecimal totalAmount;
    private String status;
    
    // 和 PO 完全一样，只是名字不同
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    // ... 其他 getter/setter
}

// ==================== OrderVO ====================
public class OrderVO {
    private Long id;
    private String customerId;
    private BigDecimal totalAmount;
    private String status;
    
    // 和 PO、BO 完全一样
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    // ... 其他 getter/setter
}

// ==================== OrderService ====================
@Service
public class OrderService {
    
    @Autowired
    private OrderDao orderDao;
    
    /**
     * 支付订单
     * 问题：业务逻辑全在 Service 层
     */
    public OrderVO payOrder(Long orderId) {
        // 1. 查询 PO
        OrderPO po = orderDao.findById(orderId);
        if (po == null) {
            throw new RuntimeException("订单不存在");
        }
        
        // 2. PO → BO（无意义的转换）
        OrderBO bo = new OrderBO();
        bo.setId(po.getId());
        bo.setCustomerId(po.getCustomerId());
        bo.setTotalAmount(po.getTotalAmount());
        bo.setStatus(po.getStatus());
        
        // 3. 业务逻辑在 Service 层
        if (!"CREATED".equals(bo.getStatus())) {
            throw new RuntimeException("订单状态不允许支付");
        }
        bo.setStatus("PAID");  // 直接修改状态
        
        // 4. BO → PO（无意义的转换）
        po.setStatus(bo.getStatus());
        orderDao.update(po);
        
        // 5. PO → VO（无意义的转换）
        OrderVO vo = new OrderVO();
        vo.setId(po.getId());
        vo.setCustomerId(po.getCustomerId());
        vo.setTotalAmount(po.getTotalAmount());
        vo.setStatus(po.getStatus());
        
        return vo;
    }
}

// ==================== OrderController ====================
@RestController
@RequestMapping("/orders")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @PostMapping("/{id}/pay")
    public OrderVO payOrder(@PathVariable Long id) {
        return orderService.payOrder(id);
    }
}
```

### 问题总结

❌ **PO、BO、VO 结构完全一样**
- 只是名字不同
- 大量重复代码
- 互相转换毫无意义

❌ **贫血模型**
- 所有对象只有 getter/setter
- 没有任何业务行为

❌ **业务逻辑在 Service**
- Service 承担所有业务逻辑
- 对象只是数据容器

❌ **技术语言**
- `setStatus("PAID")` 不是业务语言
- 容易出错（可以设置任何字符串）

❌ **无法保护不变式**
- 可以随意修改状态
- 总价可能和商品不一致

---

## 真 DDD

### 代码结构

```
interfaces/
  └── OrderController.java
application/
  └── OrderAppService.java
  └── command/
      └── PayOrderCommand.java
domain/
  └── model/
      ├── Order.java          ← 聚合根（有行为）
      ├── OrderId.java        ← 值对象
      ├── OrderItem.java      ← 值对象
      ├── Money.java          ← 值对象
      └── OrderStatus.java    ← 枚举
  └── event/
      └── OrderPaid.java      ← 领域事件
  └── repository/
      └── OrderRepository.java ← 仓储接口
infrastructure/
  └── persistence/
      └── OrderRepositoryImpl.java
```

### 完整代码

```java
// ==================== 值对象：Money ====================
package domain.model;

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
    
    public Money multiply(int quantity) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)));
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
}

// ==================== 值对象：OrderItem ====================
package domain.model;

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
    
    // 有行为！计算小计
    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }
    
    public String getSkuId() { return skuId; }
    public int getQuantity() { return quantity; }
    public Money getUnitPrice() { return unitPrice; }
}

// ==================== 枚举：OrderStatus ====================
package domain.model;

public enum OrderStatus {
    CREATED("待支付"),
    PAID("已支付"),
    CANCELED("已取消");
    
    private final String description;
    
    OrderStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

// ==================== 聚合根：Order ====================
package domain.model;

public class Order {
    private final OrderId id;
    private final String customerId;
    private final List<OrderItem> items;
    private final Money total;
    private OrderStatus status;
    
    /**
     * 构造函数：保护不变式
     */
    public Order(OrderId id, String customerId, List<OrderItem> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("订单必须至少有一个商品");
        }
        this.id = id;
        this.customerId = customerId;
        this.items = new ArrayList<>(items);
        this.total = calculateTotal();  // 自动计算，保证一致性
        this.status = OrderStatus.CREATED;
    }
    
    /**
     * 业务方法：支付订单
     * 注意：不是 setStatus，而是 pay
     */
    public void pay() {
        // 业务规则在领域对象内部
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException(
                "只有待支付订单才能支付，当前状态：" + status.getDescription()
            );
        }
        this.status = OrderStatus.PAID;
    }
    
    /**
     * 业务方法：取消订单
     */
    public void cancel() {
        if (status == OrderStatus.PAID) {
            throw new IllegalStateException("已支付订单不能取消");
        }
        if (status == OrderStatus.CANCELED) {
            throw new IllegalStateException("订单已取消");
        }
        this.status = OrderStatus.CANCELED;
    }
    
    /**
     * 私有方法：计算总价
     */
    private Money calculateTotal() {
        return items.stream()
            .map(OrderItem::subtotal)
            .reduce(Money.ZERO, Money::add);
    }
    
    // 只有 getter，没有 setter
    public OrderId getId() { return id; }
    public String getCustomerId() { return customerId; }
    public Money getTotal() { return total; }
    public OrderStatus getStatus() { return status; }
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);  // 不可修改
    }
}

// ==================== 应用服务：只做编排 ====================
package application;

@Service
public class OrderAppService {
    
    private final OrderRepository orderRepository;
    
    public OrderAppService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
    
    /**
     * 用例：支付订单
     * 注意：只做编排，业务逻辑在 Order 内部
     */
    @Transactional
    public void payOrder(String orderId) {
        // 1. 加载聚合
        Order order = orderRepository.findById(new OrderId(orderId))
            .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        
        // 2. 执行业务逻辑（在聚合内部）
        order.pay();  // 业务语言！
        
        // 3. 保存聚合
        orderRepository.save(order);
    }
}

// ==================== Controller ====================
package interfaces;

@RestController
@RequestMapping("/orders")
public class OrderController {
    
    private final OrderAppService orderAppService;
    
    public OrderController(OrderAppService orderAppService) {
        this.orderAppService = orderAppService;
    }
    
    @PostMapping("/{id}/pay")
    public ResponseEntity<Void> payOrder(@PathVariable String id) {
        orderAppService.payOrder(id);
        return ResponseEntity.ok().build();
    }
}
```

### 优势总结

✅ **充血模型**
- Order 有业务方法（`pay()`, `cancel()`）
- 不只是数据容器

✅ **业务逻辑在领域层**
- 状态流转逻辑在 Order 内部
- Service 只做编排

✅ **业务语言**
- `order.pay()` 而不是 `setStatus("PAID")`
- 代码说业务的话

✅ **类型安全**
- 使用枚举 `OrderStatus` 而不是字符串
- 不会出现拼写错误

✅ **保护不变式**
- 总价由构造函数计算，保证一致性
- 不能随意修改状态

✅ **易于测试**
- Order 可以独立测试，不需要数据库
- 业务逻辑清晰

---

## 核心区别对比

| 维度 | 假 DDD (PO/BO/VO) | 真 DDD |
|------|------------------|--------|
| **对象数量** | 3个（PO/BO/VO） | 多个（Entity/ValueObject/Aggregate） |
| **对象结构** | 完全一样 | 不同职责，不同结构 |
| **对象特征** | 只有数据 | 有行为 |
| **业务逻辑** | Service 层 | 领域对象内部 |
| **状态修改** | `setStatus("PAID")` | `order.pay()` |
| **类型安全** | 字符串 | 枚举/值对象 |
| **对象转换** | PO↔BO↔VO | 领域对象↔DTO |
| **代码重复** | 大量重复 | 最小化 |
| **测试** | 需要数据库 | 纯对象测试 |
| **语言** | 技术语言 | 业务语言 |

---

## 代码行数对比

### 假 DDD
- OrderPO: 30 行（getter/setter）
- OrderBO: 30 行（getter/setter）
- OrderVO: 30 行（getter/setter）
- OrderService: 80 行（所有业务逻辑 + 对象转换）
- **总计：170 行**

### 真 DDD
- Money: 30 行（值对象 + 业务规则）
- OrderItem: 25 行（值对象 + 业务规则）
- OrderStatus: 10 行（枚举）
- Order: 80 行（聚合根 + 所有业务逻辑）
- OrderAppService: 20 行（只做编排）
- **总计：165 行**

**代码量差不多，但质量完全不同！**

---

## 运行时对比

### 假 DDD：支付订单的调用链

```
Controller.payOrder(id)
    ↓
Service.payOrder(id)
    ↓ 查询 PO
    ↓ PO → BO（无意义转换）
    ↓ if (!"CREATED".equals(bo.getStatus()))  ← 业务逻辑
    ↓ bo.setStatus("PAID")                    ← 业务逻辑
    ↓ BO → PO（无意义转换）
    ↓ 保存 PO
    ↓ PO → VO（无意义转换）
    ↓
返回 VO
```

**问题：**
- 3 次对象转换
- 业务逻辑在 Service
- 大量重复代码

### 真 DDD：支付订单的调用链

```
Controller.payOrder(id)
    ↓
AppService.payOrder(id)
    ↓ 加载聚合 Order
    ↓ order.pay()  ← 业务逻辑在这里
    ↓ 保存聚合 Order
    ↓
返回
```

**优势：**
- 没有无意义的对象转换
- 业务逻辑在领域对象
- 代码清晰简洁

---

## 如何判断你的项目是真 DDD 还是假 DDD？

### 3 个快速问题

#### 1. 你的对象有行为吗？

**假 DDD：**
```java
public class OrderBO {
    private String status;
    public void setStatus(String status) {
        this.status = status;
    }
}
```

**真 DDD：**
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

#### 2. 你的业务逻辑在哪里？

**假 DDD：**
```java
// Service 层
if (!"CREATED".equals(order.getStatus())) {
    throw new RuntimeException("不能支付");
}
order.setStatus("PAID");
```

**真 DDD：**
```java
// 领域层
public void pay() {
    if (status != OrderStatus.CREATED) {
        throw new IllegalStateException("不能支付");
    }
    this.status = OrderStatus.PAID;
}
```

#### 3. 你的代码说业务的话吗？

**假 DDD：**
```java
order.setStatus("PAID");  // 技术语言
```

**真 DDD：**
```java
order.pay();  // 业务语言
```

---

## 总结

### 假 DDD = 传统三层架构 + 对象命名规范

```
PO/BO/VO + 贫血模型 + Service 层业务逻辑
≠ DDD
```

### 真 DDD = 领域驱动设计

```
Entity/ValueObject/Aggregate + 充血模型 + 领域层业务逻辑
= DDD
```

### 记住

**如果你的项目：**
- ✅ 有 PO/BO/VO，但它们结构完全一样
- ✅ 对象只有 getter/setter
- ✅ 业务逻辑在 Service 层
- ✅ 大量的对象转换

**那不是 DDD，只是披着 DDD 外衣的传统架构！**

**真正的 DDD：**
- ✅ 对象有行为，不只是数据
- ✅ 业务逻辑在领域对象内部
- ✅ 代码说业务的语言
- ✅ 用聚合保护业务不变式

---

## 下一步

1. 阅读 `ddd-misconceptions.md` 了解更多误解
2. 阅读 `ddd-learning-guide.md` 学习真正的 DDD
3. 阅读 `ddd-hands-on-tutorial.md` 动手实践
4. 查看项目中的 `order` 和 `course` 示例代码
