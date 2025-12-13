# 诊断你的项目：是真 DDD 还是假 DDD？

## 你说的情况

> "我之前做的项目一直都是分层：基础层、Service 层、应用层、领域层"

让我帮你诊断一下你的项目到底是哪种情况。

---

## 场景 1：最常见的"假 DDD"

### 你的项目结构可能是这样：

```
com.example.project/
│
├── domain/                    ← "领域层"
│   ├── Order.java
│   ├── OrderItem.java
│   └── User.java
│
├── service/                   ← Service 层
│   ├── OrderService.java
│   ├── UserService.java
│   └── PaymentService.java
│
├── application/               ← "应用层"
│   ├── OrderAppService.java
│   └── UserAppService.java
│
├── infrastructure/            ← "基础设施层"
│   ├── dao/
│   │   ├── OrderDao.java
│   │   └── UserDao.java
│   └── mapper/
│       └── OrderMapper.xml
│
└── controller/
    ├── OrderController.java
    └── UserController.java
```

### 你的代码可能是这样：

```java
// ==================== domain/Order.java ====================
package com.example.domain;

public class Order {
    private Long id;
    private String customerId;
    private BigDecimal totalAmount;
    private String status;  // "CREATED", "PAID", "CANCELED"
    private List<OrderItem> items;
    
    // 只有 getter/setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { 
        this.totalAmount = totalAmount; 
    }
    
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    
    // ... 其他 getter/setter
}

// ==================== service/OrderService.java ====================
package com.example.service;

@Service
public class OrderService {
    
    @Autowired
    private OrderDao orderDao;
    
    @Autowired
    private UserDao userDao;
    
    /**
     * 创建订单
     * 注意：所有业务逻辑都在这里！
     */
    public Long createOrder(String customerId, List<OrderItemDto> items) {
        // 业务规则1：检查用户
        User user = userDao.findById(customerId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 业务规则2：检查商品
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("订单必须至少有一个商品");
        }
        
        // 创建订单
        Order order = new Order();
        order.setCustomerId(customerId);
        order.setStatus("CREATED");
        
        // 业务规则3：计算总价
        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemDto dto : items) {
            if (dto.getQuantity() <= 0) {
                throw new RuntimeException("商品数量必须大于0");
            }
            
            OrderItem item = new OrderItem();
            item.setSkuId(dto.getSkuId());
            item.setQuantity(dto.getQuantity());
            item.setUnitPrice(dto.getUnitPrice());
            orderItems.add(item);
            
            BigDecimal subtotal = dto.getUnitPrice()
                .multiply(BigDecimal.valueOf(dto.getQuantity()));
            total = total.add(subtotal);
        }
        
        order.setItems(orderItems);
        order.setTotalAmount(total);
        
        orderDao.insert(order);
        return order.getId();
    }
    
    /**
     * 支付订单
     * 注意：业务逻辑在 Service 层！
     */
    public void payOrder(Long orderId) {
        Order order = orderDao.findById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        
        // 业务规则：状态检查
        if (!"CREATED".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不允许支付");
        }
        
        // 修改状态
        order.setStatus("PAID");
        orderDao.update(order);
        
        // 其他业务逻辑
        // 扣减库存
        // 发送通知
        // ...
    }
    
    /**
     * 取消订单
     */
    public void cancelOrder(Long orderId) {
        Order order = orderDao.findById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        
        // 业务规则：状态检查（重复代码）
        if ("PAID".equals(order.getStatus())) {
            throw new RuntimeException("已支付订单不能取消");
        }
        
        if ("CANCELED".equals(order.getStatus())) {
            throw new RuntimeException("订单已取消");
        }
        
        order.setStatus("CANCELED");
        orderDao.update(order);
    }
}

// ==================== application/OrderAppService.java ====================
package com.example.application;

@Service
public class OrderAppService {
    
    @Autowired
    private OrderService orderService;
    
    /**
     * 应用层只是转发调用
     * 没有实际作用！
     */
    public Long createOrder(String customerId, List<OrderItemDto> items) {
        return orderService.createOrder(customerId, items);
    }
    
    public void payOrder(Long orderId) {
        orderService.payOrder(orderId);
    }
    
    public void cancelOrder(Long orderId) {
        orderService.cancelOrder(orderId);
    }
}

// ==================== controller/OrderController.java ====================
package com.example.controller;

@RestController
@RequestMapping("/orders")
public class OrderController {
    
    @Autowired
    private OrderAppService orderAppService;
    
    @PostMapping
    public ResponseEntity<Long> createOrder(@RequestBody CreateOrderRequest request) {
        Long orderId = orderAppService.createOrder(
            request.getCustomerId(), 
            request.getItems()
        );
        return ResponseEntity.ok(orderId);
    }
    
    @PostMapping("/{id}/pay")
    public ResponseEntity<Void> payOrder(@PathVariable Long id) {
        orderAppService.payOrder(id);
        return ResponseEntity.ok().build();
    }
}
```

### 这是假 DDD！

**特征：**
- ✅ 有四层（领域层、Service 层、应用层、基础设施层）
- ❌ 领域层只有 getter/setter（贫血模型）
- ❌ 业务逻辑全在 Service 层
- ❌ 应用层只是转发调用，没有实际作用
- ❌ 使用字符串表示状态（`"CREATED"`, `"PAID"`）
- ❌ 可以随意调用 `setStatus()`，绕过业务规则

**调用链：**
```
Controller → AppService → Service → Dao
                            ↑
                      业务逻辑在这里
```

**本质：** 传统三层架构 + 多加了一个应用层

---

## 场景 2：真正的 DDD

### 正确的项目结构：

```
com.example.project/
│
├── domain/                           ← 领域层（核心）
│   ├── model/
│   │   ├── Order.java               ← 聚合根（有行为）
│   │   ├── OrderId.java             ← 值对象
│   │   ├── OrderItem.java           ← 值对象
│   │   ├── Money.java               ← 值对象
│   │   └── OrderStatus.java         ← 枚举
│   ├── event/
│   │   ├── OrderPlaced.java         ← 领域事件
│   │   └── OrderPaid.java
│   └── repository/
│       └── OrderRepository.java     ← 仓储接口
│
├── application/                      ← 应用层
│   ├── OrderAppService.java         ← 用例编排
│   ├── command/
│   │   ├── CreateOrderCommand.java
│   │   └── PayOrderCommand.java
│   └── query/
│       └── OrderQueryService.java
│
├── infrastructure/                   ← 基础设施层
│   ├── persistence/
│   │   ├── OrderRepositoryImpl.java
│   │   └── OrderJpaEntity.java
│   └── config/
│       └── DataInitializer.java
│
└── interfaces/                       ← 接口层
    └── web/
        ├── OrderController.java
        └── dto/
            ├── CreateOrderRequest.java
            └── OrderResponse.java
```

### 正确的代码：

```java
// ==================== domain/model/Money.java ====================
package com.example.domain.model;

public class Money {
    public static final Money ZERO = new Money(BigDecimal.ZERO);
    
    private final BigDecimal amount;
    
    public Money(BigDecimal amount) {
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("金额最多2位小数");
        }
        this.amount = amount;
    }
    
    public Money(String amount) {
        this(new BigDecimal(amount));
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

// ==================== domain/model/OrderItem.java ====================
package com.example.domain.model;

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

// ==================== domain/model/OrderStatus.java ====================
package com.example.domain.model;

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

// ==================== domain/model/Order.java ====================
package com.example.domain.model;

public class Order {
    private final OrderId id;
    private final String customerId;
    private final List<OrderItem> items;
    private final Money total;
    private OrderStatus status;
    
    private final List<Object> pendingEvents = new ArrayList<>();
    
    /**
     * 构造函数：保护不变式
     */
    public Order(OrderId id, String customerId, List<OrderItem> items) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("客户ID不能为空");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("订单必须至少有一个商品");
        }
        
        this.id = id;
        this.customerId = customerId;
        this.items = new ArrayList<>(items);
        this.total = calculateTotal();  // 自动计算
        this.status = OrderStatus.CREATED;
        
        pendingEvents.add(new OrderPlaced(id, customerId, total));
    }
    
    /**
     * 业务方法：支付订单
     * 注意：业务逻辑在领域对象内部！
     */
    public void pay() {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException(
                "只有待支付订单才能支付，当前状态：" + status.getDescription()
            );
        }
        
        this.status = OrderStatus.PAID;
        pendingEvents.add(new OrderPaid(id, total));
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
        return Collections.unmodifiableList(items);
    }
    
    public List<Object> getPendingEvents() {
        return Collections.unmodifiableList(pendingEvents);
    }
    
    public void clearPendingEvents() {
        pendingEvents.clear();
    }
}

// ==================== application/OrderAppService.java ====================
package com.example.application;

@Service
public class OrderAppService {
    
    private final OrderRepository orderRepository;
    
    public OrderAppService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
    
    /**
     * 用例：创建订单
     * 注意：只做编排，业务逻辑在 Order 内部
     */
    @Transactional
    public String createOrder(CreateOrderCommand command) {
        // 1. 构建值对象
        List<OrderItem> items = command.items().stream()
            .map(dto -> new OrderItem(
                dto.skuId(),
                dto.quantity(),
                new Money(dto.unitPrice())
            ))
            .collect(Collectors.toList());
        
        // 2. 创建聚合（业务规则在构造函数中）
        Order order = new Order(
            OrderId.generate(),
            command.customerId(),
            items
        );
        
        // 3. 保存聚合
        orderRepository.save(order);
        
        // 4. 发布事件
        order.clearPendingEvents();
        
        return order.getId().getValue();
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
        order.pay();
        
        // 3. 保存聚合
        orderRepository.save(order);
        
        // 4. 发布事件
        order.clearPendingEvents();
    }
    
    /**
     * 用例：取消订单
     */
    @Transactional
    public void cancelOrder(String orderId) {
        Order order = orderRepository.findById(new OrderId(orderId))
            .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        
        order.cancel();
        orderRepository.save(order);
    }
}

// ==================== interfaces/web/OrderController.java ====================
package com.example.interfaces.web;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private final OrderAppService orderAppService;
    
    public OrderController(OrderAppService orderAppService) {
        this.orderAppService = orderAppService;
    }
    
    @PostMapping
    public ResponseEntity<String> createOrder(@RequestBody CreateOrderRequest request) {
        CreateOrderCommand command = new CreateOrderCommand(
            request.getCustomerId(),
            request.getItems()
        );
        String orderId = orderAppService.createOrder(command);
        return ResponseEntity.ok(orderId);
    }
    
    @PostMapping("/{id}/pay")
    public ResponseEntity<Void> payOrder(@PathVariable String id) {
        orderAppService.payOrder(id);
        return ResponseEntity.ok().build();
    }
}
```

### 这是真 DDD！

**特征：**
- ✅ 有四层，但职责清晰
- ✅ 领域层有业务方法（充血模型）
- ✅ 业务逻辑在领域对象内部
- ✅ 应用层做编排和事务管理
- ✅ 使用枚举表示状态（类型安全）
- ✅ 没有 setter，只能通过业务方法修改状态
- ✅ 有值对象（Money, OrderItem）
- ✅ 有领域事件

**调用链：**
```
Controller → AppService → Domain Model
                              ↑
                        业务逻辑在这里
```

**本质：** 真正的领域驱动设计

---

## 对比总结

| 维度 | 假 DDD（你的项目？） | 真 DDD |
|------|---------------------|--------|
| **分层** | 领域层、Service 层、应用层、基础设施层 | 领域层、应用层、基础设施层、接口层 |
| **领域层对象** | 只有 getter/setter | 有业务方法 |
| **业务逻辑位置** | Service 层 | 领域对象内部 |
| **应用层职责** | 转发调用 | 编排用例、管理事务 |
| **Service 层** | 有，承担业务逻辑 | 没有（或领域服务） |
| **状态类型** | 字符串 `"CREATED"` | 枚举 `OrderStatus.CREATED` |
| **状态修改** | `setStatus("PAID")` | `order.pay()` |
| **值对象** | 没有 | 有（Money, OrderItem） |
| **总价计算** | 手动在 Service 层 | 自动在构造函数 |
| **领域事件** | 没有 | 有 |

---

## 快速自测

### 测试 1：打开你的 Order.java

**看看有没有这些方法：**

```java
// 假 DDD 有这些
public void setStatus(String status) { ... }
public void setTotalAmount(BigDecimal amount) { ... }

// 真 DDD 有这些
public void pay() { ... }
public void cancel() { ... }
```

**如果只有 setter，没有业务方法 → 假 DDD**

### 测试 2：找到支付订单的代码

**看看业务逻辑在哪里：**

```java
// 假 DDD：业务逻辑在 Service 层
@Service
public class OrderService {
    public void payOrder(Long orderId) {
        Order order = orderDao.findById(orderId);
        if (!"CREATED".equals(order.getStatus())) {  // ← 业务逻辑
            throw new RuntimeException("不能支付");
        }
        order.setStatus("PAID");
        orderDao.update(order);
    }
}

// 真 DDD：业务逻辑在领域对象
public class Order {
    public void pay() {  // ← 业务逻辑
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("不能支付");
        }
        this.status = OrderStatus.PAID;
    }
}
```

**如果业务逻辑在 Service 层 → 假 DDD**

### 测试 3：看你的应用层

**看看应用层做什么：**

```java
// 假 DDD：只是转发
@Service
public class OrderAppService {
    @Autowired
    private OrderService orderService;
    
    public void payOrder(Long orderId) {
        orderService.payOrder(orderId);  // 只是转发
    }
}

// 真 DDD：编排用例
@Service
public class OrderAppService {
    @Transactional
    public void payOrder(String orderId) {
        Order order = orderRepository.findById(orderId);
        order.pay();  // 调用领域对象
        orderRepository.save(order);
    }
}
```

**如果应用层只是转发 → 假 DDD**

---

## 你的项目可能是这样的

### 90% 的可能性：假 DDD

```
你的项目 = 传统三层架构 + 多加了一个应用层 + 改了包名
```

**特征：**
- 有"领域层"，但只是数据容器
- 有 Service 层，承担所有业务逻辑
- 有"应用层"，但只是转发调用
- 本质还是传统架构

### 10% 的可能性：真 DDD

```
你的项目 = 真正的领域驱动设计
```

**特征：**
- 领域层有业务逻辑
- 应用层做编排
- 没有单独的 Service 层（或领域服务）

---

## 如何确认？

### 给我看 3 段代码

1. **你的 Order.java（或类似的领域对象）**
2. **你的 OrderService.java（如果有）**
3. **你的 OrderAppService.java（如果有）**

我就能告诉你是真 DDD 还是假 DDD。

---

## 如果是假 DDD，怎么办？

### 不要慌，这很正常

- 90% 的公司说自己用 DDD，但实际上是假 DDD
- 这不是你的问题，是行业普遍现象
- 重要的是认识到问题，然后改进

### 重构步骤

1. **识别业务逻辑**：找出 Service 层的业务规则
2. **移动到领域对象**：把业务逻辑移到 Order 内部
3. **提取值对象**：Money, OrderItem 等
4. **简化应用层**：只做编排，不包含业务逻辑
5. **去掉 Service 层**：或者改成领域服务

---

## 总结

### 你的项目很可能是：

```
领域层（贫血模型）
    ↓
Service 层（业务逻辑）
    ↓
应用层（转发调用）
    ↓
基础设施层

= 假 DDD（只是分层）
```

### 真正的 DDD 应该是：

```
领域层（充血模型，业务逻辑）
    ↑
应用层（编排用例）
    ↑
接口层
    ↓
基础设施层

= 真 DDD
```

### 记住

**分层不等于 DDD！**

关键是：
- 业务逻辑在领域层
- 对象有行为
- 代码说业务的语言

---

## 下一步

1. 检查你的项目代码
2. 确认是真 DDD 还是假 DDD
3. 如果是假 DDD，参考重构步骤改进
4. 查看项目中的 `order` 和 `course` 示例
5. 阅读 `ddd-hands-on-tutorial.md` 学习真正的 DDD
