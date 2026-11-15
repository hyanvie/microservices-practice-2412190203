package com.zjgsu.hyh.service;

import com.zjgsu.hyh.client.ItemClient;
import com.zjgsu.hyh.client.ItemView;
import feign.FeignException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final ItemClient itemClient;
    private final Map<String, OrderView> orders = new ConcurrentHashMap<>();
    private final JdbcTemplate jdbcTemplate;

    public OrderService(ItemClient itemClient) {
        this(itemClient, null);
    }

    @Autowired
    public OrderService(ItemClient itemClient, JdbcTemplate jdbcTemplate) {
        this.itemClient = itemClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    public OrderView createOrder(String itemId, String buyerId) {
        if (itemId == null || itemId.isBlank() || buyerId == null || buyerId.isBlank()) {
            throw new InvalidOrderException("itemId 和 buyerId 不能为空");
        }

        ItemView item = fetchItem(itemId);
        if (!item.onSale()) {
            throw new InvalidOrderException("商品当前不可购买：" + itemId);
        }

        OrderView order = new OrderView(
                UUID.randomUUID().toString(),
                item.id(),
                buyerId,
                item.sellerId(),
                item.price(),
                "PENDING_PAYMENT",
                Instant.now());
        if (jdbcTemplate != null) {
            jdbcTemplate.update(
                    "INSERT INTO orders (id, item_id, buyer_id, seller_id, price, status, created_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    order.id(),
                    order.itemId(),
                    order.buyerId(),
                    order.sellerId(),
                    order.price(),
                    order.status(),
                    order.createdAt());
        } else {
            orders.put(order.id(), order);
        }
        return order;
    }

    public OrderView getOrder(String orderId) {
        if (jdbcTemplate != null) {
            return jdbcTemplate.query(
                    "SELECT id, item_id, buyer_id, seller_id, price, status, created_at "
                            + "FROM orders WHERE id = ?",
                    (resultSet, rowNum) -> new OrderView(
                            resultSet.getString("id"),
                            resultSet.getString("item_id"),
                            resultSet.getString("buyer_id"),
                            resultSet.getString("seller_id"),
                            resultSet.getBigDecimal("price"),
                            resultSet.getString("status"),
                            resultSet.getTimestamp("created_at").toInstant()),
                    orderId).stream().findFirst().orElseThrow(() -> new OrderNotFoundException(orderId));
        }
        OrderView order = orders.get(orderId);
        if (order == null) {
            throw new OrderNotFoundException(orderId);
        }
        return order;
    }

    private ItemView fetchItem(String itemId) {
        try {
            ItemView item = itemClient.getItem(itemId);
            if (item == null) {
                throw new DependencyUnavailableException("商品服务返回了空响应");
            }
            return item;
        } catch (FeignException.NotFound exception) {
            throw new InvalidOrderException("商品不存在：" + itemId, exception);
        } catch (FeignException exception) {
            throw new DependencyUnavailableException("商品服务暂时不可用", exception);
        }
    }

    public record OrderView(
            String id,
            String itemId,
            String buyerId,
            String sellerId,
            BigDecimal price,
            String status,
            Instant createdAt) {
    }

    public static class InvalidOrderException extends RuntimeException {

        public InvalidOrderException(String message) {
            super(message);
        }

        public InvalidOrderException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class DependencyUnavailableException extends RuntimeException {

        public DependencyUnavailableException(String message) {
            super(message);
        }

        public DependencyUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class OrderNotFoundException extends RuntimeException {

        public OrderNotFoundException(String orderId) {
            super("订单不存在：" + orderId);
        }
    }
}
