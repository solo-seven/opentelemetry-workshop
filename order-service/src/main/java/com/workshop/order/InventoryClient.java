package com.workshop.order;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class InventoryClient {

    private final RestTemplate restTemplate;
    private final String inventoryBaseUrl;

    public InventoryClient(RestTemplate restTemplate,
                           @Value("${inventory.service.url}") String inventoryBaseUrl) {
        this.restTemplate = restTemplate;
        this.inventoryBaseUrl = inventoryBaseUrl;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> lookup(String id) {
        String url = inventoryBaseUrl + "/inventory/" + id;
        return restTemplate.getForObject(url, Map.class);
    }
}
