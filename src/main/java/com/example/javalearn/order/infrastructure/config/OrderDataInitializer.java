package com.example.javalearn.order.infrastructure.config;

import com.example.javalearn.order.domain.model.CustomerId;
import com.example.javalearn.order.domain.model.Money;
import com.example.javalearn.order.domain.model.Order;
import com.example.javalearn.order.domain.model.OrderId;
import com.example.javalearn.order.domain.model.OrderItem;
import com.example.javalearn.order.infrastructure.persistence.InMemoryOrderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class OrderDataInitializer {

    @Bean
    CommandLineRunner seedOrders(InMemoryOrderRepository repository) {
        return args -> {
            Order sample = Order.place(
                    new OrderId("order-100"),
                    new CustomerId("c-1"),
                    List.of(
                            new OrderItem("sku-apple", 2, new Money(new BigDecimal("3.50"))),
                            new OrderItem("sku-pen", 1, new Money(new BigDecimal("1.20")))
                    )
            );
            repository.put(sample);
            sample.clearPendingEvents();
        };
    }
}
