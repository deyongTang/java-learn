package com.example.javalearn.order.interfaces.web;

import com.example.javalearn.order.application.OrderAppService;
import com.example.javalearn.order.application.command.PlaceOrderCommand;
import com.example.javalearn.order.application.view.OrderView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderAppService orderAppService;

    public OrderController(OrderAppService orderAppService) {
        this.orderAppService = orderAppService;
    }

    @PostMapping
    public ResponseEntity<String> placeOrder(@RequestBody PlaceOrderRequest request) {
        PlaceOrderCommand command = new PlaceOrderCommand(
                request.customerId(),
                request.items().stream()
                        .map(i -> new PlaceOrderCommand.Item(i.skuId(), i.quantity(), i.unitPrice()))
                        .toList()
        );
        var id = orderAppService.place(command);
        return ResponseEntity.ok(id.getValue());
    }

    @PostMapping("/{orderId}/pay")
    public ResponseEntity<Void> pay(@PathVariable String orderId) {
        orderAppService.pay(orderId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{orderId}")
    public OrderView get(@PathVariable String orderId) {
        return orderAppService.get(orderId);
    }

    public record PlaceOrderRequest(String customerId, List<LineItem> items) {
    }

    public record LineItem(String skuId, int quantity, java.math.BigDecimal unitPrice) {
    }
}
