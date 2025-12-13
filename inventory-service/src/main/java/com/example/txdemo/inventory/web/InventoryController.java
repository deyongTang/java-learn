package com.example.txdemo.inventory.web;

import com.example.txdemo.inventory.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/seed")
    public ResponseEntity<?> seed(@RequestBody SeedRequest request) {
        inventoryService.seed(request.productId(), request.available());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reserve")
    public ResponseEntity<?> reserve(@RequestBody ReserveRequest request) {
        try {
            inventoryService.reserveWithLock(request.productId(), request.quantity());
            return ResponseEntity.ok().build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(ex.getMessage());
        }
    }

    @PostMapping("/release")
    public ResponseEntity<?> release(@RequestBody ReleaseRequest request) {
        inventoryService.release(request.productId(), request.quantity());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{productId}")
    public ResponseEntity<?> get(@PathVariable String productId) {
        return inventoryService.get(productId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record SeedRequest(String productId, int available) {}

    public record ReserveRequest(String productId, int quantity) {}

    public record ReleaseRequest(String productId, int quantity) {}
}

