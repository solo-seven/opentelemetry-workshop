package com.workshop.order;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    private final OrderService orderService;
    private final InventoryClient inventoryClient;

    public OrderController(OrderService orderService, InventoryClient inventoryClient) {
        this.orderService = orderService;
        this.inventoryClient = inventoryClient;
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable String id) {
        Order order = new Order(id);
        orderService.validateOrder(order);

        Map<String, Object> inventory = inventoryClient.lookup(id);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderId", order.getId());
        body.put("customerTier", order.getCustomerTier());
        body.put("inventory", inventory);
        return ResponseEntity.ok(body);
    }
}
