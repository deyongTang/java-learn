# DDD（领域驱动设计）完整学习指南

## 第一部分：DDD 是什么？为什么需要 DDD？

### 1.1 传统开发的痛点

想象你在开发一个电商系统，传统的做法可能是：

```
Controller → Service → DAO → Database
```

**问题来了：**
- 业务逻辑散落在 Service 层，到处都是 `if-else`
- 数据模型（Entity）只是数据容器，没有行为，变成"贫血模型"
- 当业务复杂时，Service 变成"上帝类"，几千行代码
- 技术人员和业务人员说的不是同一种语言
- 需求变更时，不知道改哪里，牵一发动全身

**举个例子：**
```java
// 传统贫血模型
public class Order {
    private Long id;
    private BigDecimal amount;
    private String status;
    // 只有 getter/setter，没有业务逻辑
}

// Service 层承担所有逻辑
public class OrderService {
    public void pay(Long orderId) {
        Order order = orderDao.findById(orderId);
        if (order.getStatus().equals("CREATED")) {
            order.setStatus("PAID");
            // 计算金额
            // 检查库存
            // 发送通知
            // ... 100 行代码
            orderDao.save(order);
        }
    }
}
```

### 1.2 DDD 的核心思想

**DDD 的本质：让代码说业务的语言，让软件模型反映业务模型。**

核心理念：
1. **统一语言（Ubiquitous Language）**：技术和业务用同一套术语
2. **领域模型（Domain Model）**：用对象表达业务概念和规则
3. **限界上下文（Bounded Context）**：划分业务边界，避免大泥球
4. **聚合（Aggregate）**：保护业务不变式，确保数据一致性

**DDD 改造后：**
```java
// 充血模型：对象有行为
public class Order {
    private OrderId id;
    private Money total;
    private OrderStatus status;
    private List<OrderItem> items;
    
    // 业务逻辑在领域对象内部
    public void pay() {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("只有待支付订单才能支付");
        }
        this.status = OrderStatus.PAID;
        // 发布领域事件
        addEvent(new OrderPaid(this.id));
    }
    
    public Money calculateTotal() {
        return items.stream()
            .map(OrderItem::subtotal)
            .reduce(Money.ZERO, Money::add);
    }
}
```

---

## 第二部分：DDD 的战略设计（宏观）

### 2.1 限界上下文（Bounded Context）

**问题：** 在大型系统中，"订单"在不同场景下含义不同：
- 销售上下文：订单 = 客户购买意向
- 物流上下文：订单 = 配送任务
- 财务上下文：订单 = 应收账款

**解决方案：** 划分限界上下文，每个上下文有自己的模型

```
┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
│  订单上下文      │      │  库存上下文      │      │  支付上下文      │
│                 │      │                 │      │                 │
│  Order          │─────▶│  Stock          │      │  Payment        │
│  OrderItem      │      │  Reservation    │◀─────│  Transaction    │
└─────────────────┘      └─────────────────┘      └─────────────────┘
```

### 2.2 统一语言（Ubiquitous Language）

**坏例子：**
- 程序员说："我们需要更新 order 表的 status 字段"
- 业务说："客户完成支付后，订单就确认了"

**好例子：**
- 大家都说："当订单支付成功时，订单状态从'待支付'变为'已支付'"
- 代码中：`order.pay()` 而不是 `order.setStatus("PAID")`

---

## 第三部分：DDD 的战术设计（微观）

### 3.1 实体（Entity）

**特征：** 有唯一标识，生命周期中属性可变，但身份不变

```java
public class Course {
    private final CourseId id;  // 唯一标识
    private String name;        // 可以改名
    private int capacity;       // 可以扩容
    
    // 两个课程相等，看的是 ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Course)) return false;
        Course course = (Course) o;
        return Objects.equals(id, course.id);
    }
}
```

### 3.2 值对象（Value Object）

**特征：** 没有标识，不可变，通过属性判断相等

```java
public class Money {
    private final BigDecimal amount;
    private final String currency;
    
    public Money(BigDecimal amount, String currency) {
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("金额最多2位小数");
        }
        this.amount = amount;
        this.currency = currency;
    }
    
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("货币类型不同");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    // 值对象相等看属性
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Money)) return false;
        Money money = (Money) o;
        return amount.equals(money.amount) && currency.equals(money.currency);
    }
}
```

**为什么用值对象？**
- 封装业务规则（金额必须2位小数）
- 类型安全（不会把金额和数量搞混）
- 不可变，线程安全

### 3.3 聚合（Aggregate）

**核心概念：** 一组相关对象的集合，有一个聚合根，保护业务不变式

```java
public class Order {  // 聚合根
    private OrderId id;
    private List<OrderItem> items;  // 聚合内部对象
    private Money total;
    private OrderStatus status;
    
    // 不变式：订单必须至少有一个商品
    public Order(OrderId id, List<OrderItem> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("订单至少要有一个商品");
        }
        this.id = id;
        this.items = new ArrayList<>(items);
        this.total = calculateTotal();
        this.status = OrderStatus.CREATED;
    }
    
    // 不变式：只有待支付订单才能支付
    public void pay() {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("订单状态不允许支付");
        }
        this.status = OrderStatus.PAID;
    }
    
    // 外部不能直接修改 items，必须通过聚合根
    public void addItem(OrderItem item) {
        items.add(item);
        this.total = calculateTotal();  // 保持总价一致
    }
}
```

**聚合的规则：**
1. 外部只能通过聚合根访问聚合内部对象
2. 聚合根负责维护不变式（业务规则）
3. 一个事务只修改一个聚合
4. 聚合之间通过 ID 引用，不直接持有对象

### 3.4 领域服务（Domain Service）

**什么时候用？** 当业务逻辑不属于任何一个实体时

```java
// 转账涉及两个账户，不属于任何一个
public class TransferService {
    public void transfer(Account from, Account to, Money amount) {
        from.debit(amount);  // 扣款
        to.credit(amount);   // 入账
    }
}
```

### 3.5 领域事件（Domain Event）

**作用：** 记录领域中发生的重要事实，解耦不同聚合

```java
public class OrderPaid {  // 领域事件
    private final OrderId orderId;
    private final Money amount;
    private final Instant paidAt;
    
    public OrderPaid(OrderId orderId, Money amount) {
        this.orderId = orderId;
        this.amount = amount;
        this.paidAt = Instant.now();
    }
}

// 订单支付后，发布事件
public class Order {
    public void pay() {
        this.status = OrderStatus.PAID;
        addEvent(new OrderPaid(this.id, this.total));
    }
}

// 其他上下文监听事件
@EventListener
public void onOrderPaid(OrderPaid event) {
    // 库存上下文：扣减库存
    // 物流上下文：创建配送单
    // 积分上下文：增加积分
}
```

### 3.6 仓储（Repository）

**作用：** 封装聚合的持久化，让领域层不依赖数据库

```java
// 接口定义在领域层
public interface OrderRepository {
    Optional<Order> findById(OrderId id);
    void save(Order order);
    void delete(OrderId id);
}

// 实现在基础设施层
public class JpaOrderRepository implements OrderRepository {
    @Override
    public void save(Order order) {
        // JPA 持久化逻辑
    }
}
```

---

## 第四部分：DDD 分层架构

```
┌─────────────────────────────────────────┐
│  接口层 (Interfaces)                     │  ← HTTP/RPC/消息队列
│  Controller, DTO, Assembler             │
├─────────────────────────────────────────┤
│  应用层 (Application)                    │  ← 用例编排、事务
│  AppService, Command, Query             │
├─────────────────────────────────────────┤
│  领域层 (Domain)                         │  ← 核心业务逻辑
│  Entity, ValueObject, Aggregate         │
│  DomainService, Event, Repository接口   │
├─────────────────────────────────────────┤
│  基础设施层 (Infrastructure)             │  ← 技术实现
│  Repository实现, 消息队列, 缓存          │
└─────────────────────────────────────────┘
```

**依赖方向：** 上层依赖下层，领域层不依赖任何层

---

## 第五部分：实战案例对比

### 案例：课程报名系统

**需求：**
- 学生可以报名课程
- 课程有容量限制
- 同一学生不能重复报名

### 传统写法（贫血模型）

```java
// 数据对象
public class Course {
    private String id;
    private String name;
    private int capacity;
    private int enrolled;
    // getter/setter
}

// Service 承担所有逻辑
@Service
public class CourseService {
    public void enroll(String courseId, String studentId) {
        Course course = courseDao.findById(courseId);
        
        // 检查是否已报名
        if (enrollmentDao.exists(courseId, studentId)) {
            throw new RuntimeException("已经报名了");
        }
        
        // 检查容量
        if (course.getEnrolled() >= course.getCapacity()) {
            throw new RuntimeException("课程已满");
        }
        
        // 创建报名记录
        Enrollment enrollment = new Enrollment();
        enrollment.setCourseId(courseId);
        enrollment.setStudentId(studentId);
        enrollmentDao.save(enrollment);
        
        // 更新已报名人数
        course.setEnrolled(course.getEnrolled() + 1);
        courseDao.update(course);
    }
}
```

**问题：**
- 业务规则散落在 Service
- Course 只是数据容器
- 容易出现并发问题（两个人同时报名最后一个名额）

### DDD 写法（充血模型）

```java
// 聚合根：封装业务规则
public class Course {
    private final CourseId id;
    private final String name;
    private final int capacity;
    private final Set<StudentId> enrolled;  // 聚合内部管理
    
    // 业务逻辑在领域对象内
    public CourseEnrolled enroll(StudentId studentId) {
        // 不变式1：不能重复报名
        if (enrolled.contains(studentId)) {
            throw new IllegalStateException("学生已报名该课程");
        }
        
        // 不变式2：不能超过容量
        if (enrolled.size() >= capacity) {
            throw new IllegalStateException("课程已满");
        }
        
        enrolled.add(studentId);
        return new CourseEnrolled(id, studentId);  // 领域事件
    }
    
    public int availableSeats() {
        return capacity - enrolled.size();
    }
}

// 应用服务：编排用例
@Service
public class CourseAppService {
    private final CourseRepository courseRepository;
    
    @Transactional
    public void enroll(EnrollCommand command) {
        Course course = courseRepository.findById(new CourseId(command.courseId()))
            .orElseThrow(() -> new IllegalArgumentException("课程不存在"));
        
        CourseEnrolled event = course.enroll(new StudentId(command.studentId()));
        courseRepository.save(course);
        
        // 发布事件（可选）
        eventPublisher.publish(event);
    }
}
```

**优势：**
- 业务规则在 Course 内部，一目了然
- Course 是"活"的对象，有行为
- 聚合保证一致性，Repository 以聚合为单位保存
- 并发问题由数据库事务 + 聚合锁解决

---

## 第六部分：何时使用 DDD？

### 适合 DDD 的场景

✅ **复杂业务逻辑**
- 电商订单、金融交易、保险理赔
- 业务规则多，状态流转复杂

✅ **需要长期演进**
- 需求经常变化
- 需要多团队协作

✅ **业务是核心竞争力**
- 业务创新驱动
- 需要业务和技术深度协作

### 不适合 DDD 的场景

❌ **简单 CRUD**
- 博客系统、内容管理
- 没有复杂业务规则

❌ **技术驱动的系统**
- 监控系统、日志系统
- 重点是性能和可靠性

❌ **一次性项目**
- 短期项目，不需要长期维护

---

## 第七部分：DDD 常见误区

### 误区1：DDD = 四层架构
❌ 错误：只要分了四层就是 DDD
✅ 正确：DDD 的核心是领域模型，不是分层

### 误区2：所有对象都要是聚合根
❌ 错误：每个实体都独立保存
✅ 正确：聚合要尽量小，只包含必须一起修改的对象

### 误区3：过度设计
❌ 错误：简单的 CRUD 也用 DDD
✅ 正确：根据复杂度选择合适的方法

### 误区4：领域层依赖框架
❌ 错误：领域对象继承 JPA Entity
✅ 正确：领域层纯 POJO，框架在基础设施层

---

## 第八部分：学习路径

### 阶段1：理解概念（1-2周）
- 阅读《领域驱动设计》（蓝皮书）第1-3章
- 理解统一语言、限界上下文、聚合

### 阶段2：实践战术设计（2-4周）
- 用 DDD 重构一个小项目
- 重点练习：实体、值对象、聚合、仓储
- 参考本项目的 `order` 和 `course` 示例

### 阶段3：掌握战略设计（1-2个月）
- 学习事件风暴（Event Storming）
- 练习划分限界上下文
- 理解上下文映射（Context Mapping）

### 阶段4：高级主题（持续）
- CQRS（命令查询分离）
- Event Sourcing（事件溯源）
- 微服务与 DDD

---

## 第九部分：推荐资源

### 书籍
1. 《领域驱动设计》（Eric Evans）- 经典，但较难
2. 《实现领域驱动设计》（Vaughn Vernon）- 更实战
3. 《领域驱动设计精粹》（Vaughn Vernon）- 快速入门

### 在线资源
- Domain-Driven Design Community: https://dddcommunity.org/
- Awesome DDD: https://github.com/heynickc/awesome-ddd

---

## 总结

**DDD 的本质：**
1. 用代码表达业务，而不是数据库表
2. 让对象有行为，而不只是数据容器
3. 保护业务规则，而不是到处 if-else
4. 用统一语言，让业务和技术无缝沟通

**记住：DDD 不是银弹，但当业务复杂时，它能让你的代码更清晰、更易维护、更贴近业务。**

下一步：查看 `docs/ddd-hands-on-tutorial.md` 进行实战练习！
