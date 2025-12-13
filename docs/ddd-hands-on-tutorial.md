# DDD 实战教程：从零构建一个图书借阅系统

## 教程目标

通过构建一个图书借阅系统，让你亲手实践 DDD 的核心概念：
- 识别聚合和聚合根
- 设计值对象
- 实现业务不变式
- 使用领域事件
- 理解分层架构

---

## 第一步：理解业务需求（统一语言）

### 业务场景

图书馆管理员需要一个系统来管理图书借阅：

**核心概念（统一语言）：**
- **图书（Book）**：有 ISBN、书名、作者，可以被借出和归还
- **借阅（Borrowing）**：记录谁在什么时候借了什么书
- **读者（Reader）**：有借阅限额（最多借3本），有信用等级
- **逾期（Overdue）**：借阅超过14天未归还

**业务规则（不变式）：**
1. 一本书同时只能被一个人借阅
2. 读者最多同时借3本书
3. 信用等级为"黑名单"的读者不能借书
4. 借阅期限14天，逾期需要支付罚金

---

## 第二步：识别聚合

### 思考：哪些对象应该放在一起？

**错误的划分：**
```
Book 聚合：Book
Reader 聚合：Reader
Borrowing 聚合：Borrowing
```
问题：借书时需要同时修改 Book、Reader、Borrowing，跨聚合事务

**正确的划分：**

**聚合1：Book（图书聚合）**
- 聚合根：Book
- 职责：管理图书的借出和归还状态
- 不变式：一本书同时只能被一个人借

**聚合2：Reader（读者聚合）**
- 聚合根：Reader
- 内部对象：Borrowing（借阅记录）
- 职责：管理读者的借阅记录和限额
- 不变式：最多借3本，黑名单不能借

**为什么这样划分？**
- Book 和 Borrowing 不需要强一致性（可以通过事件最终一致）
- Reader 和 Borrowing 需要强一致性（检查限额时必须准确）

---

## 第三步：设计值对象

### ISBN（国际标准书号）

```java
package com.example.javalearn.library.domain.model;

import java.util.Objects;

/**
 * 值对象：ISBN
 * 特征：不可变，通过值判断相等，有业务规则
 */
public class ISBN {
    private final String value;
    
    public ISBN(String value) {
        if (value == null || !value.matches("\\d{13}")) {
            throw new IllegalArgumentException("ISBN 必须是13位数字");
        }
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ISBN)) return false;
        ISBN isbn = (ISBN) o;
        return value.equals(isbn.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
```

### BorrowingPeriod（借阅期限）

```java
package com.example.javalearn.library.domain.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 值对象：借阅期限
 * 封装借阅时间的业务规则
 */
public class BorrowingPeriod {
    private static final int STANDARD_DAYS = 14;
    
    private final LocalDate borrowedAt;
    private final LocalDate dueDate;
    
    public BorrowingPeriod(LocalDate borrowedAt) {
        this.borrowedAt = borrowedAt;
        this.dueDate = borrowedAt.plusDays(STANDARD_DAYS);
    }
    
    public boolean isOverdue() {
        return LocalDate.now().isAfter(dueDate);
    }
    
    public long overdueDays() {
        if (!isOverdue()) {
            return 0;
        }
        return ChronoUnit.DAYS.between(dueDate, LocalDate.now());
    }
    
    public LocalDate getBorrowedAt() {
        return borrowedAt;
    }
    
    public LocalDate getDueDate() {
        return dueDate;
    }
}
```

### CreditLevel（信用等级）

```java
package com.example.javalearn.library.domain.model;

/**
 * 值对象：信用等级
 */
public enum CreditLevel {
    NORMAL("正常"),
    BLACKLIST("黑名单");
    
    private final String description;
    
    CreditLevel(String description) {
        this.description = description;
    }
    
    public boolean canBorrow() {
        return this != BLACKLIST;
    }
    
    public String getDescription() {
        return description;
    }
}
```

---

## 第四步：实现聚合根 - Book

```java
package com.example.javalearn.library.domain.model;

import com.example.javalearn.library.domain.event.BookBorrowed;
import com.example.javalearn.library.domain.event.BookReturned;

import java.util.ArrayList;
import java.util.List;

/**
 * 聚合根：图书
 * 职责：管理图书的借出和归还
 */
public class Book {
    private final ISBN isbn;
    private final String title;
    private final String author;
    private BookStatus status;
    private ReaderId currentBorrower;  // 当前借阅者（如果已借出）
    
    private final List<Object> pendingEvents = new ArrayList<>();
    
    public Book(ISBN isbn, String title, String author) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("书名不能为空");
        }
        if (author == null || author.isBlank()) {
            throw new IllegalArgumentException("作者不能为空");
        }
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.status = BookStatus.AVAILABLE;
    }
    
    /**
     * 借出图书
     * 不变式：只有可借状态的书才能被借出
     */
    public void borrow(ReaderId borrower) {
        if (status != BookStatus.AVAILABLE) {
            throw new IllegalStateException("图书当前不可借");
        }
        this.status = BookStatus.BORROWED;
        this.currentBorrower = borrower;
        pendingEvents.add(new BookBorrowed(isbn, borrower));
    }
    
    /**
     * 归还图书
     * 不变式：只有已借出的书才能归还
     */
    public void returnBook() {
        if (status != BookStatus.BORROWED) {
            throw new IllegalStateException("图书未被借出");
        }
        ReaderId previousBorrower = this.currentBorrower;
        this.status = BookStatus.AVAILABLE;
        this.currentBorrower = null;
        pendingEvents.add(new BookReturned(isbn, previousBorrower));
    }
    
    public boolean isAvailable() {
        return status == BookStatus.AVAILABLE;
    }
    
    // Getters
    public ISBN getIsbn() {
        return isbn;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public BookStatus getStatus() {
        return status;
    }
    
    public ReaderId getCurrentBorrower() {
        return currentBorrower;
    }
    
    public List<Object> getPendingEvents() {
        return List.copyOf(pendingEvents);
    }
    
    public void clearPendingEvents() {
        pendingEvents.clear();
    }
}

enum BookStatus {
    AVAILABLE,  // 可借
    BORROWED    // 已借出
}
```

---

## 第五步：实现聚合根 - Reader

```java
package com.example.javalearn.library.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 聚合根：读者
 * 职责：管理读者的借阅记录和限额
 */
public class Reader {
    private static final int MAX_BORROWING_LIMIT = 3;
    
    private final ReaderId id;
    private final String name;
    private CreditLevel creditLevel;
    private final List<Borrowing> activeBorrowings;  // 聚合内部对象
    
    public Reader(ReaderId id, String name, CreditLevel creditLevel) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("读者姓名不能为空");
        }
        this.id = id;
        this.name = name;
        this.creditLevel = creditLevel;
        this.activeBorrowings = new ArrayList<>();
    }
    
    /**
     * 借书
     * 不变式1：黑名单用户不能借书
     * 不变式2：最多同时借3本书
     * 不变式3：不能重复借同一本书
     */
    public Borrowing borrowBook(ISBN isbn) {
        // 检查信用等级
        if (!creditLevel.canBorrow()) {
            throw new IllegalStateException("信用等级不允许借书");
        }
        
        // 检查借阅限额
        if (activeBorrowings.size() >= MAX_BORROWING_LIMIT) {
            throw new IllegalStateException("已达到借阅上限（" + MAX_BORROWING_LIMIT + "本）");
        }
        
        // 检查是否重复借阅
        if (activeBorrowings.stream().anyMatch(b -> b.getIsbn().equals(isbn))) {
            throw new IllegalStateException("已经借阅了这本书");
        }
        
        Borrowing borrowing = new Borrowing(isbn, new BorrowingPeriod(java.time.LocalDate.now()));
        activeBorrowings.add(borrowing);
        return borrowing;
    }
    
    /**
     * 还书
     */
    public void returnBook(ISBN isbn) {
        Borrowing borrowing = activeBorrowings.stream()
            .filter(b -> b.getIsbn().equals(isbn))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("未找到该借阅记录"));
        
        activeBorrowings.remove(borrowing);
    }
    
    /**
     * 检查是否有逾期
     */
    public boolean hasOverdueBooks() {
        return activeBorrowings.stream().anyMatch(Borrowing::isOverdue);
    }
    
    /**
     * 拉黑（业务操作）
     */
    public void blacklist() {
        this.creditLevel = CreditLevel.BLACKLIST;
    }
    
    public int availableQuota() {
        return MAX_BORROWING_LIMIT - activeBorrowings.size();
    }
    
    // Getters
    public ReaderId getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public CreditLevel getCreditLevel() {
        return creditLevel;
    }
    
    public List<Borrowing> getActiveBorrowings() {
        return Collections.unmodifiableList(activeBorrowings);
    }
}
```

### Borrowing（聚合内部对象）

```java
package com.example.javalearn.library.domain.model;

/**
 * 实体（聚合内部）：借阅记录
 * 注意：Borrowing 不是聚合根，它属于 Reader 聚合
 */
public class Borrowing {
    private final ISBN isbn;
    private final BorrowingPeriod period;
    
    public Borrowing(ISBN isbn, BorrowingPeriod period) {
        this.isbn = isbn;
        this.period = period;
    }
    
    public boolean isOverdue() {
        return period.isOverdue();
    }
    
    public long overdueDays() {
        return period.overdueDays();
    }
    
    public ISBN getIsbn() {
        return isbn;
    }
    
    public BorrowingPeriod getPeriod() {
        return period;
    }
}
```

---

## 第六步：定义领域事件

```java
package com.example.javalearn.library.domain.event;

import com.example.javalearn.library.domain.model.ISBN;
import com.example.javalearn.library.domain.model.ReaderId;

import java.time.Instant;

/**
 * 领域事件：图书被借出
 */
public class BookBorrowed {
    private final ISBN isbn;
    private final ReaderId borrower;
    private final Instant occurredAt;
    
    public BookBorrowed(ISBN isbn, ReaderId borrower) {
        this.isbn = isbn;
        this.borrower = borrower;
        this.occurredAt = Instant.now();
    }
    
    public ISBN getIsbn() {
        return isbn;
    }
    
    public ReaderId getBorrower() {
        return borrower;
    }
    
    public Instant getOccurredAt() {
        return occurredAt;
    }
}
```

```java
package com.example.javalearn.library.domain.event;

import com.example.javalearn.library.domain.model.ISBN;
import com.example.javalearn.library.domain.model.ReaderId;

import java.time.Instant;

/**
 * 领域事件：图书被归还
 */
public class BookReturned {
    private final ISBN isbn;
    private final ReaderId returner;
    private final Instant occurredAt;
    
    public BookReturned(ISBN isbn, ReaderId returner) {
        this.isbn = isbn;
        this.returner = returner;
        this.occurredAt = Instant.now();
    }
    
    public ISBN getIsbn() {
        return isbn;
    }
    
    public ReaderId getReturner() {
        return returner;
    }
    
    public Instant getOccurredAt() {
        return occurredAt;
    }
}
```

---

## 第七步：定义仓储接口（领域层）

```java
package com.example.javalearn.library.domain.repository;

import com.example.javalearn.library.domain.model.Book;
import com.example.javalearn.library.domain.model.ISBN;

import java.util.Optional;

/**
 * 仓储接口：图书仓储
 * 定义在领域层，实现在基础设施层
 */
public interface BookRepository {
    Optional<Book> findByIsbn(ISBN isbn);
    void save(Book book);
}
```

```java
package com.example.javalearn.library.domain.repository;

import com.example.javalearn.library.domain.model.Reader;
import com.example.javalearn.library.domain.model.ReaderId;

import java.util.Optional;

/**
 * 仓储接口：读者仓储
 */
public interface ReaderRepository {
    Optional<Reader> findById(ReaderId id);
    void save(Reader reader);
}
```

---

## 第八步：实现应用服务（用例编排）

```java
package com.example.javalearn.library.application;

import com.example.javalearn.library.application.command.BorrowBookCommand;
import com.example.javalearn.library.application.command.ReturnBookCommand;
import com.example.javalearn.library.domain.model.*;
import com.example.javalearn.library.domain.repository.BookRepository;
import com.example.javalearn.library.domain.repository.ReaderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 应用服务：图书借阅用例
 * 职责：编排领域对象，管理事务，不包含业务逻辑
 */
@Service
public class LibraryAppService {
    
    private final BookRepository bookRepository;
    private final ReaderRepository readerRepository;
    
    public LibraryAppService(BookRepository bookRepository, ReaderRepository readerRepository) {
        this.bookRepository = bookRepository;
        this.readerRepository = readerRepository;
    }
    
    /**
     * 用例：借书
     * 步骤：
     * 1. 加载聚合
     * 2. 执行业务逻辑
     * 3. 保存聚合
     * 4. 发布事件（可选）
     */
    @Transactional
    public void borrowBook(BorrowBookCommand command) {
        // 1. 加载聚合
        Book book = bookRepository.findByIsbn(new ISBN(command.isbn()))
            .orElseThrow(() -> new IllegalArgumentException("图书不存在"));
        
        Reader reader = readerRepository.findById(new ReaderId(command.readerId()))
            .orElseThrow(() -> new IllegalArgumentException("读者不存在"));
        
        // 2. 执行业务逻辑（在聚合内部）
        reader.borrowBook(book.getIsbn());  // Reader 聚合检查限额
        book.borrow(reader.getId());         // Book 聚合更新状态
        
        // 3. 保存聚合
        readerRepository.save(reader);
        bookRepository.save(book);
        
        // 4. 发布事件（简化版：直接清理）
        book.clearPendingEvents();
    }
    
    /**
     * 用例：还书
     */
    @Transactional
    public void returnBook(ReturnBookCommand command) {
        Book book = bookRepository.findByIsbn(new ISBN(command.isbn()))
            .orElseThrow(() -> new IllegalArgumentException("图书不存在"));
        
        Reader reader = readerRepository.findById(new ReaderId(command.readerId()))
            .orElseThrow(() -> new IllegalArgumentException("读者不存在"));
        
        reader.returnBook(book.getIsbn());
        book.returnBook();
        
        readerRepository.save(reader);
        bookRepository.save(book);
        
        book.clearPendingEvents();
    }
}
```

---

## 第九步：对比分析

### 传统方式 vs DDD 方式

**场景：借书时检查限额**

**传统方式：**
```java
// Service 层承担所有逻辑
public void borrowBook(String isbn, String readerId) {
    // 检查图书状态
    Book book = bookDao.findByIsbn(isbn);
    if (book.getStatus().equals("BORROWED")) {
        throw new RuntimeException("图书已被借出");
    }
    
    // 检查读者限额
    int count = borrowingDao.countByReaderId(readerId);
    if (count >= 3) {
        throw new RuntimeException("已达到借阅上限");
    }
    
    // 检查信用等级
    Reader reader = readerDao.findById(readerId);
    if (reader.getCreditLevel().equals("BLACKLIST")) {
        throw new RuntimeException("信用等级不允许借书");
    }
    
    // 更新数据
    book.setStatus("BORROWED");
    bookDao.update(book);
    
    Borrowing borrowing = new Borrowing();
    borrowing.setIsbn(isbn);
    borrowing.setReaderId(readerId);
    borrowingDao.save(borrowing);
}
```

**DDD 方式：**
```java
// 应用服务：编排
@Transactional
public void borrowBook(BorrowBookCommand command) {
    Book book = bookRepository.findByIsbn(new ISBN(command.isbn()))
        .orElseThrow(() -> new IllegalArgumentException("图书不存在"));
    
    Reader reader = readerRepository.findById(new ReaderId(command.readerId()))
        .orElseThrow(() -> new IllegalArgumentException("读者不存在"));
    
    // 业务逻辑在聚合内部
    reader.borrowBook(book.getIsbn());  // 自动检查限额和信用
    book.borrow(reader.getId());         // 自动检查状态
    
    readerRepository.save(reader);
    bookRepository.save(book);
}

// Reader 聚合内部
public Borrowing borrowBook(ISBN isbn) {
    if (!creditLevel.canBorrow()) {
        throw new IllegalStateException("信用等级不允许借书");
    }
    if (activeBorrowings.size() >= MAX_BORROWING_LIMIT) {
        throw new IllegalStateException("已达到借阅上限");
    }
    // ...
}
```

**对比：**
- 传统方式：业务规则散落在 Service，难以维护
- DDD 方式：业务规则在聚合内部，清晰明确

---

## 第十步：练习题

### 练习1：添加预约功能

**需求：** 当图书被借出时，其他读者可以预约，归还后优先借给预约者

**思考：**
1. 预约（Reservation）应该放在哪个聚合？
2. 需要哪些新的值对象？
3. 需要哪些新的领域事件？

### 练习2：实现罚金计算

**需求：** 逾期每天罚款1元

**思考：**
1. 罚金计算逻辑应该放在哪里？
2. 是否需要新的值对象 `Fine`？
3. 还书时如何触发罚金计算？

### 练习3：添加图书分类

**需求：** 图书有分类（小说、技术、历史等），不同分类借阅期限不同

**思考：**
1. 分类（Category）是实体还是值对象？
2. 如何修改 `BorrowingPeriod` 来支持不同期限？

---

## 总结

通过这个实战案例，你应该理解了：

1. **聚合的划分**：根据业务不变式和事务边界
2. **值对象的作用**：封装业务规则，类型安全
3. **聚合根的职责**：保护不变式，对外提供业务方法
4. **应用服务的定位**：编排用例，不包含业务逻辑
5. **领域事件的价值**：解耦聚合，实现最终一致性

**关键要点：**
- 业务逻辑在领域层（聚合内部）
- 应用层只做编排和事务管理
- 仓储以聚合为单位
- 一个事务只修改一个聚合（Book 和 Reader 分别保存）

**下一步：**
- 尝试实现练习题
- 查看项目中的 `order` 和 `course` 示例
- 用 DDD 重构你自己的项目
