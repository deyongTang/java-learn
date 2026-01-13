package com.example.shardingdemo.controller;

import com.example.shardingdemo.domain.Order;
import com.example.shardingdemo.service.OrderService;
import com.example.shardingdemo.trace.TraceIdHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Order create(@RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request.getOrderId(), request.getUserId(), request.getAmount());
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> findById(@PathVariable long orderId) {
        return orderService.findById(orderId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Order> findByUserId(@RequestParam long userId,
                                    @RequestParam(defaultValue = "10") int limit) {
        return orderService.findByUserId(userId, limit);
    }

    @GetMapping("/trace-demo")
    public String traceDemo() {
        String traceId = TraceIdHolder.get();
        orderService.asyncTraceDemo();
        return "traceId=" + traceId;
    }

    public static class CreateOrderRequest {
        private Long orderId;
        private Long userId;
        private BigDecimal amount;

        public CreateOrderRequest() {
        }

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }
}
