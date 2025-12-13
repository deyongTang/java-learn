# DDD vs 传统开发：深度对比

## 案例：电商订单系统

我们用同一个需求，分别用传统方式和 DDD 方式实现，让你看到本质区别。

---

## 需求描述

**业务场景：** 用户下单购买商品

**业务规则：**
1. 订单必须至少有一个商品
2. 订单总价 = 所有商品小计之和
3. 订单创建后状态为"待支付"
4. 只有"待支付"的订单才能支付
5. 支付后状态变为"已支付"
6. 已支付的订单不能取消

---

## 方式一：传统三层架构（贫血模型）

### 1. 数据模型（只有数据，没有行为）

```java
// Entity：只是数据容器
@Entity
@Table(name = "orders")
public class Order {
    @Id
    private Long id;
    
    private String customerId;
    
    private BigDecimal totalAmount;
    
    private String status;  // "CREATED", "PAID", "CANCELED"
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items;
    
    // 只有 getter/setter，没有业务逻辑
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    private Long id;
    
    @ManyToOne
    private Order order;
    
    private String skuId;
    private Integer quantity;
    private BigDecimal unitPrice;
    
    // 只有 getter/setter
}
```

### 2. Service 层（承担所有业务逻辑）

```java
@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    /**
     * 创建订单
     * 问题：业务逻辑散落在 Service 层
     */
    @Transactional
    public Long createOrder(CreateOrderRequest request) {
        // 校验：至少有一个商品
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("订单必须至少有一个商品");
        }
        
        // 创建订单
        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setStatus("CREATED");
        
        // 创建订单项
        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        
        for (CreateOrderRequest.ItemDto itemDto : request.getItems()) {
            // 校验数量
            if (itemDto.getQuantity() <= 0) {
                throw new RuntimeException("商品数量必须大于0");
            }
            
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setSkuId(itemDto.getSkuId());
            item.setQuantity(itemDto.getQuantity());
            item.setUnitPrice(itemDto.getUnitPrice());
            items.add(item);
            
            // 计算小计
            BigDecimal subtotal = itemDto.getUnitPrice()
                .multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            total = total.add(subtotal);
        }
        
        order.setItems(items);
        order.setTotalAmount(total);
        
        orderRepository.save(order);
        return order.getId();
    }
    
    /**
     * 支付订单
     * 问题：状态流转逻辑在 Service 层，容易出错
     */
    @Transactional
    public void payOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("订单不存在"));
        
        // 检查状态
        if (!"CREATED".equals(order.getStatus())) {
            throw new RuntimeException("只有待支付订单才能支付");
        }
        
        // 更新状态
        order.setStatus("PAID");
        orderRepository.save(order);
        
        // 发送通知、扣减库存等
        // ...
    }
    
    /**
     * 取消订单
     * 问题：业务规则重复，难以维护
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("订单不存在"));
        
        // 检查状态
        if ("PAID".equals(order.getStatus())) {
            throw new RuntimeException("已支付订单不能取消");
        }
        
        if ("CANCELED".equals(order.getStatus())) {
            throw new RuntimeException("订单已取消");
        }
        
        order.setStatus("CANCELED");
        orderRepository.save(order);
    }
}
```

### 3. Controller 层

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @PostMapping
    public ResponseEntity<Long> createOrder(@RequestBody CreateOrderRequest request) {
        Long orderId = orderService.createOrder(request);
        return ResponseEntity.ok(orderId);
    }
    
    @PostMapping("/{orderId}/pay")
    public ResponseEntity<Void> payOrder(@PathVariable Long orderId) {
        orderService.payOrder(orderId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.ok().build();
    }
}
```

### 传统方式的问题

❌ **问题1：贫血模型**
- `Order` 只是数据容器，没有行为
- 所有业务逻辑在 Service 层

❌ **问题2：业务规则散落**
- 状态检查逻辑重复出现
- 修改规则时需要改多处

❌ **问题3：容易出错**
- 可以直接 `order.setStatus("XXX")`，绕过业务规则
- 总价可能和商品小计不一致

❌ **问题4：难以测试**
- 测试 Service 需要 Mock Repository
- 业务逻辑和技术细节耦合

❌ **问题5：不符合业务语言**
- 代码中是 `setStatus("PAID")`
- 业务说的是"支付订单"

---

## 方式二：DDD（充血模型）

### 1. 值对象（封装业务规则）

```java
package com.example.javalearn.order.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 值对象：金额
 * 特征：不可变，有业务规则，类型安全
 */
public class Money {
    public static final Money ZERO = new Money(BigDecimal.ZERO);
    
    private final BigDecimal amount;
    
    public Money(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("金额不能为空");
        }
        // 业务规则：金额保留2位小数
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }
    
    public Money(String amount) {
        this(new BigDecimal(amount));
    }
    
    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }
    
    public Money multiply(int quantity) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)));
    }
    
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money)) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(amount);
    }
    
    @Override
    public String toString() {
        return amount.toString();
    }
}
```

```java
package com.example.javalearn.order.domain.model;

import java.util.Objects;

/**
 * 值对象：订单项
 * 特征：不可变，封装小计计算逻辑
 */
public class OrderItem {
    private final String skuId;
    private final int quantity;
    private final Money unitPrice;
    
    public OrderItem(String skuId, int quantity, Money unitPrice) {
        if (skuId == null || skuId.isBlank()) {
            throw new IllegalArgumentException("SKU ID 不能为空");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("数量必须大于0");
        }
        if (unitPrice == null || !unitPrice.isPositive()) {
            throw new IllegalArgumentException("单价必须大于0");
        }
        
        this.skuId = skuId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }
    
    /**
     * 计算小计（业务逻辑在值对象内部）
     */
    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }
    
    public String getSkuId() {
        return skuId;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public Money getUnitPrice() {
        return unitPrice;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItem)) return false;
        OrderItem orderItem = (OrderItem) o;
        return quantity == orderItem.quantity &&
               skuId.equals(orderItem.skuId) &&
               unitPrice.equals(orderItem.unitPrice);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(skuId, quantity, unitPrice);
    }
}
```

```java
package com.example.javalearn.order.domain.model;

/**
 * 值对象：订单状态
 * 使用枚举而不是字符串，类型安全
 */
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
```

### 2. 聚合根（封装业务逻辑）

```java
package com.example.javalearn.order.domain.model;

import com.example.javalearn.order.domain.event.OrderPaid;
import com.example.javalearn.order.domain.event.OrderPlaced;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 聚合根：订单
 * 职责：
 * 1. 保护业务不变式
 * 2. 提供业务方法（而不是 setter）
 * 3. 产生领域事件
 */
public class Order {
    private final OrderId id;
    private final String customerId;
    private final List<OrderItem> items;
    private final Money total;
    private OrderStatus status;
    
    private final List<Object> pendingEvents = new ArrayList<>();
    
    /**
     * 构造函数：创建订单
     * 不变式1：订单必须至少有一个商品
     * 不变式2：总价必须等于商品小计之和
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
        this.items = new ArrayList<>(items);  // 防御性复制
        this.total = calculateTotal();
        this.status = OrderStatus.CREATED;
        
        // 产生领域事件
        pendingEvents.add(new OrderPlaced(id, customerId, total));
    }
    
    /**
     * 业务方法：支付订单
     * 不变式：只有待支付订单才能支付
     * 
     * 注意：方法名是业务语言，不是技术语言
     */
    public void pay() {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("只有待支付订单才能支付，当前状态：" + status.getDescription());
        }
        
        this.status = OrderStatus.PAID;
        pendingEvents.add(new OrderPaid(id, total));
    }
    
    /**
     * 业务方法：取消订单
     * 不变式：已支付订单不能取消
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
     * 计算总价（私有方法，保证一致性）
     */
    private Money calculateTotal() {
        return items.stream()
            .map(OrderItem::subtotal)
            .reduce(Money.ZERO, Money::add);
    }
    
    /**
     * 查询方法：是否可以取消
     */
    public boolean canCancel() {
        return status == OrderStatus.CREATED;
    }
    
    // Getters（只读，不提供 setter）
    public OrderId getId() {
        return id;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);  // 不可修改
    }
    
    public Money getTotal() {
        return total;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public List<Object> getPendingEvents() {
        return Collections.unmodifiableList(pendingEvents);
    }
    
    public void clearPendingEvents() {
        pendingEvents.clear();
    }
}
```

### 3. 应用服务（只做编排）

```java
package com.example.javalearn.order.application;

import com.example.javalearn.order.application.command.CreateOrderCommand;
import com.example.javalearn.order.domain.model.*;
import com.example.javalearn.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 应用服务：订单用例
 * 职责：
 * 1. 编排领域对象
 * 2. 管理事务
 * 3. 发布事件
 * 
 * 不包含业务逻辑！
 */
@Service
public class OrderAppService {
    
    private final OrderRepository orderRepository;
    
    public OrderAppService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
    
    /**
     * 用例：创建订单
     * 注意：只是编排，业务逻辑在 Order 内部
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
        
        // 4. 发布事件（简化版）
        order.clearPendingEvents();
        
        return order.getId().getValue();
    }
    
    /**
     * 用例：支付订单
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
```

### 4. Controller 层（不变）

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private final OrderAppService orderAppService;
    
    public OrderController(OrderAppService orderAppService) {
        this.orderAppService = orderAppService;
    }
    
    @PostMapping
    public ResponseEntity<String> createOrder(@RequestBody CreateOrderCommand command) {
        String orderId = orderAppService.createOrder(command);
        return ResponseEntity.ok(orderId);
    }
    
    @PostMapping("/{orderId}/pay")
    public ResponseEntity<Void> payOrder(@PathVariable String orderId) {
        orderAppService.payOrder(orderId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable String orderId) {
        orderAppService.cancelOrder(orderId);
        return ResponseEntity.ok().build();
    }
}
```

### DDD 方式的优势

✅ **优势1：充血模型**
- `Order` 有行为，不只是数据容器
- 业务逻辑在领域对象内部

✅ **优势2：业务规则集中**
- 所有状态流转逻辑在 `Order` 内部
- 修改规则只需改一处

✅ **优势3：不可能出错**
- 没有 `setStatus()` 方法，只能通过 `pay()` 等业务方法
- 总价由构造函数计算，保证一致性

✅ **优势4：易于测试**
- 测试 `Order` 不需要数据库
- 纯领域逻辑测试

✅ **优势5：符合业务语言**
- 代码中是 `order.pay()`
- 和业务说的"支付订单"一致

---

## 核心对比表

| 维度 | 传统方式 | DDD 方式 |
|------|---------|---------|
| **模型** | 贫血模型（只有数据） | 充血模型（有行为） |
| **业务逻辑位置** | Service 层 | 领域对象内部 |
| **状态修改** | `setStatus("PAID")` | `order.pay()` |
| **业务规则** | 散落各处 | 集中在聚合 |
| **类型安全** | 字符串状态 | 枚举/值对象 |
| **测试** | 需要 Mock | 纯对象测试 |
| **语言** | 技术语言 | 业务语言 |
| **一致性** | 容易出错 | 聚合保证 |

---

## 实际代码行数对比

### 传统方式
- Entity: 50 行（只有 getter/setter）
- Service: 150 行（所有业务逻辑）
- **总计：200 行**

### DDD 方式
- 值对象: 100 行（Money + OrderItem + OrderStatus）
- 聚合根: 120 行（Order，包含所有业务逻辑）
- 应用服务: 50 行（只做编排）
- **总计：270 行**

**看起来 DDD 代码更多？**

是的，但是：
1. **可维护性更好**：业务规则集中，修改容易
2. **可测试性更好**：领域对象可以独立测试
3. **可扩展性更好**：添加新规则不会影响其他代码
4. **可读性更好**：代码说业务的语言

---

## 何时选择哪种方式？

### 选择传统方式
- 简单 CRUD，没有复杂业务规则
- 短期项目，不需要长期维护
- 团队不熟悉 DDD

### 选择 DDD 方式
- 复杂业务逻辑
- 需要长期演进
- 业务规则经常变化
- 多团队协作

---

## 总结

**传统方式的本质：** 面向数据库编程
- 先设计表结构
- 然后写 CRUD
- 业务逻辑是"附加"的

**DDD 的本质：** 面向业务编程
- 先理解业务
- 然后建立领域模型
- 数据库是"实现细节"

**记住：DDD 不是为了写更多代码，而是为了写更好的代码！**
