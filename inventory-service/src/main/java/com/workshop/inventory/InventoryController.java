package com.workshop.inventory;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/inventory/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable long id) {
        try {
            Item item = inventoryService.lookup(id);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("id", item.getId());
            body.put("name", item.getName());
            body.put("stock", item.getStock());
            return ResponseEntity.ok(body);
        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.status(404).body(Map.of("error", "item not found", "id", id));
        }
    }
}
