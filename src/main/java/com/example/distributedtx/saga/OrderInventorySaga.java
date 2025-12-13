package com.example.distributedtx.saga;

import com.example.distributedtx.model.Order;
import com.example.distributedtx.service.InventoryService;
import com.example.distributedtx.service.OrderService;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class OrderInventorySaga {
    private final OrderService orderService;
    private final InventoryService inventoryService;

    public OrderInventorySaga(OrderService orderService, InventoryService inventoryService) {
        this.orderService = orderService;
        this.inventoryService = inventoryService;
    }

    public boolean placeOrder(String productId, int quantity, boolean simulateInventoryFailure) {
        Order order = orderService.createOrder(productId, quantity);
        List<SagaStep> steps = List.of(
                new SagaStep("预留库存", () -> {
                    if (simulateInventoryFailure) {
                        throw new IllegalStateException("刻意制造的库存预留失败");
                    }
                    inventoryService.reserve(productId, quantity);
                }, () -> inventoryService.release(productId, quantity)),
                new SagaStep("订单标记为已预留", () -> orderService.markReserved(order), () -> orderService.cancelOrder(order))
        );

        return runSaga(steps);
    }

    private boolean runSaga(List<SagaStep> steps) {
        Deque<SagaStep> executed = new ArrayDeque<>();
        try {
            for (SagaStep step : steps) {
                System.out.printf("[Saga] 执行步骤：%s\n", step.getName());
                step.execute();
                executed.push(step);
            }
            System.out.println("[Saga] 所有步骤执行成功，事务提交\n");
            return true;
        } catch (Exception ex) {
            System.out.printf("[Saga] 步骤执行失败：%s，开始补偿\n", ex.getMessage());
            while (!executed.isEmpty()) {
                SagaStep step = executed.pop();
                System.out.printf("[Saga] 补偿：%s\n", step.getName());
                step.compensate();
            }
            System.out.println("[Saga] 补偿完成，事务已回滚\n");
            return false;
        }
    }
}
