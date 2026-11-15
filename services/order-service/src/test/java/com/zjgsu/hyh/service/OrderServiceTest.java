package com.zjgsu.hyh.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.zjgsu.hyh.client.ItemClient;
import com.zjgsu.hyh.client.ItemView;
import feign.FeignException;
import feign.Request;
import feign.Response;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class OrderServiceTest {

    @Test
    void createsOrderOnlyAfterItemServiceValidation() {
        ItemClient itemClient = mock(ItemClient.class);
        when(itemClient.getItem("item-1001")).thenReturn(
                new ItemView("item-1001", "Java 编程教材", new BigDecimal("35.00"), "seller-1001", true));
        OrderService service = new OrderService(itemClient);

        OrderService.OrderView order = service.createOrder("item-1001", "buyer-1001");

        assertEquals("item-1001", order.itemId());
        assertEquals("seller-1001", order.sellerId());
        assertEquals(new BigDecimal("35.00"), order.price());
    }

    @Test
    void mapsMissingItemToInvalidOrder() {
        ItemClient itemClient = mock(ItemClient.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(itemClient.getItem("missing")).thenThrow(feignException(404));
        OrderService service = new OrderService(itemClient, jdbcTemplate);

        assertThrows(
                OrderService.InvalidOrderException.class,
                () -> service.createOrder("missing", "buyer-1001"));
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void mapsRemoteFailureToDependencyUnavailable() {
        ItemClient itemClient = mock(ItemClient.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(itemClient.getItem("item-1001")).thenThrow(feignException(503));
        OrderService service = new OrderService(itemClient, jdbcTemplate);

        assertThrows(
                OrderService.DependencyUnavailableException.class,
                () -> service.createOrder("item-1001", "buyer-1001"));
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void doesNotWriteOrderWhenItemIsNotOnSale() {
        ItemClient itemClient = mock(ItemClient.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(itemClient.getItem("item-1001")).thenReturn(
                new ItemView("item-1001", "Java 编程教材", new BigDecimal("35.00"), "seller-1001", false));
        OrderService service = new OrderService(itemClient, jdbcTemplate);

        assertThrows(
                OrderService.InvalidOrderException.class,
                () -> service.createOrder("item-1001", "buyer-1001"));
        verifyNoInteractions(jdbcTemplate);
    }

    private static FeignException feignException(int status) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "http://item-service/api/items/item-1001",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                null);
        Response response = Response.builder()
                .request(request)
                .status(status)
                .reason("remote failure")
                .headers(Map.of())
                .body(new byte[0])
                .build();
        return FeignException.errorStatus("GET /api/items/item-1001", response);
    }
}
