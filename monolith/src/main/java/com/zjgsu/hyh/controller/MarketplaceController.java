package com.zjgsu.hyh.controller;

import com.zjgsu.hyh.model.Item;
import com.zjgsu.hyh.model.Order;
import com.zjgsu.hyh.service.ItemOrderService;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MarketplaceController {

    private final ItemOrderService service;

    public MarketplaceController(ItemOrderService service) {
        this.service = service;
    }

    @GetMapping("/items")
    public List<Item> getItems() {
        return service.getItems();
    }

    @GetMapping("/items/{itemId}")
    public Item getItem(@PathVariable String itemId) {
        return service.getItem(itemId);
    }

    @PostMapping("/orders")
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequest request) {
        Order order = service.createOrder(request.itemId(), request.buyerId());
        return ResponseEntity.created(URI.create("/api/orders/" + order.id())).body(order);
    }

    @GetMapping("/orders")
    public List<Order> getOrders(@RequestParam(required = false) String buyerId) {
        return service.getOrders(buyerId);
    }

    @GetMapping("/orders/{orderId}")
    public Order getOrder(@PathVariable String orderId) {
        return service.getOrder(orderId);
    }

    @PostMapping("/orders/{orderId}/cancel")
    public Order cancelOrder(@PathVariable String orderId) {
        return service.cancelOrder(orderId);
    }
}
