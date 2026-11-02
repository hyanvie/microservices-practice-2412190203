package com.zjgsu.hyh.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

class OrderServiceTest {

    @Test
    void createsOrderOnlyAfterItemServiceValidation() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        ServiceInstance itemInstance = mock(ServiceInstance.class);
        when(discoveryClient.getInstances("item-service")).thenReturn(List.of(itemInstance));
        when(itemInstance.getUri()).thenReturn(URI.create("http://10.0.0.8:8081"));
        OrderService service = new OrderService(restTemplate, discoveryClient);
        when(restTemplate.getForObject(
                eq(URI.create("http://10.0.0.8:8081/api/items/item-1001")),
                eq(OrderService.ItemView.class))).thenReturn(
                new OrderService.ItemView("item-1001", new BigDecimal("35.00"), "seller-1001", true));

        OrderService.OrderView order = service.createOrder("item-1001", "buyer-1001");

        assertEquals("item-1001", order.itemId());
    }

    @Test
    void doesNotWriteWhenItemServiceFails() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        ServiceInstance itemInstance = mock(ServiceInstance.class);
        when(discoveryClient.getInstances("item-service")).thenReturn(List.of(itemInstance));
        when(itemInstance.getUri()).thenReturn(URI.create("http://10.0.0.8:8081"));
        when(restTemplate.getForObject(
                URI.create("http://10.0.0.8:8081/api/items/missing"),
                OrderService.ItemView.class)).thenThrow(new ResourceAccessException("down"));

        OrderService service = new OrderService(restTemplate, discoveryClient);

        assertThrows(
                OrderService.DependencyUnavailableException.class,
                () -> service.createOrder("missing", "buyer-1001"));
        assertThrows(OrderService.OrderNotFoundException.class, () -> service.getOrder("unknown"));
    }

    @Test
    void rejectsOrderWhenItemServiceHasNoInstances() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        when(discoveryClient.getInstances("item-service")).thenReturn(List.of());

        OrderService service = new OrderService(restTemplate, discoveryClient);

        assertThrows(
                OrderService.DependencyUnavailableException.class,
                () -> service.createOrder("item-1001", "buyer-1001"));
    }

}
