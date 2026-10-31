package com.zjgsu.hyh.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

class OrderServiceTest {

    @Test
    void createsOrderOnlyAfterItemServiceValidation() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        OrderService service = new OrderService(restTemplate, "http://item-service:8081");
        when(restTemplate.getForObject(
                eq("http://item-service:8081/api/items/item-1001"),
                eq(OrderService.ItemView.class))).thenReturn(
                new OrderService.ItemView("item-1001", new BigDecimal("35.00"), "seller-1001", true));

        OrderService.OrderView order = service.createOrder("item-1001", "buyer-1001");

        assertEquals("item-1001", order.itemId());
    }

    @Test
    void doesNotWriteWhenItemServiceFails() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForObject(
                "http://item-service:8081/api/items/missing",
                OrderService.ItemView.class)).thenThrow(new ResourceAccessException("down"));

        OrderService service = new OrderService(restTemplate, "http://item-service:8081");

        assertThrows(
                OrderService.DependencyUnavailableException.class,
                () -> service.createOrder("missing", "buyer-1001"));
        assertThrows(OrderService.OrderNotFoundException.class, () -> service.getOrder("unknown"));
    }

}
