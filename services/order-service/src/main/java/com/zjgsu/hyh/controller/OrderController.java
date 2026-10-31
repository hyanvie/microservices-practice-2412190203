package com.zjgsu.hyh.controller;

import com.zjgsu.hyh.service.OrderService;
import com.zjgsu.hyh.service.OrderService.OrderView;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderView> createOrder(@RequestBody CreateOrderRequest request) {
        OrderView order = orderService.createOrder(request.itemId(), request.buyerId());
        return ResponseEntity.created(URI.create("/api/orders/" + order.id())).body(order);
    }

    @GetMapping("/{orderId}")
    public OrderView getOrder(@PathVariable String orderId) {
        return orderService.getOrder(orderId);
    }

    public record CreateOrderRequest(String itemId, String buyerId) {
    }
}
