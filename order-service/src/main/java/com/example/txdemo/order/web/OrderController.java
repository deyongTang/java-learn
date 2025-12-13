package com.example.txdemo.order.web;

import com.example.txdemo.order.domain.OrderStatus;
import com.example.txdemo.order.service.OrderService;
import com.example.txdemo.order.service.PlaceOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final PlaceOrderService placeOrderService;
    private final OrderService orderService;

    public OrderController(PlaceOrderService placeOrderService, OrderService orderService) {
        this.placeOrderService = placeOrderService;
        this.orderService = orderService;
    }

    @PostMapping("/place")
    public ResponseEntity<?> place(@RequestBody PlaceOrderRequest request) {
        try {
            String orderId = placeOrderService.place(request.productId(), request.quantity());
            return ResponseEntity.ok(Map.of("accepted", true, "orderId", orderId, "status", OrderStatus.PENDING));
        } catch (Exception ex) {
            return ResponseEntity.status(409).body(Map.of("accepted", false, "error", ex.getMessage()));
        }
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return orderService.listOrders();
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> get(@PathVariable String orderId) {
        return orderService.getOrder(orderId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record PlaceOrderRequest(String productId, int quantity) {}
}

