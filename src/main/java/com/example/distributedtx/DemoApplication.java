package com.example.distributedtx;

import com.example.distributedtx.saga.OrderInventorySaga;
import com.example.distributedtx.service.InventoryService;
import com.example.distributedtx.service.OrderService;

public class DemoApplication {

    public static void main(String[] args) {
        InventoryService inventoryService = new InventoryService();
        inventoryService.seed("product-1", 5);

        OrderService orderService = new OrderService();
        OrderInventorySaga saga = new OrderInventorySaga(orderService, inventoryService);

        System.out.println("\n============ 成功案例 ============");
        boolean success = saga.placeOrder("product-1", 2, false);
        System.out.printf("结果：%s\n", success ? "提交" : "回滚");

        System.out.println("\n============ 失败 + 补偿案例 ============");
        boolean failed = saga.placeOrder("product-1", 3, true);
        System.out.printf("结果：%s\n", failed ? "提交" : "回滚");
    }
}
