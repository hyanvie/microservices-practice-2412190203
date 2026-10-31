package com.zjgsu.hyh.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ItemService {

    private final Map<String, ItemView> items = new ConcurrentHashMap<>();
    private final JdbcTemplate jdbcTemplate;

    public ItemService() {
        this.jdbcTemplate = null;
        items.put("item-1001", new ItemView("item-1001", "Java 编程教材", new BigDecimal("35.00"), "seller-1001", true));
        items.put("item-1002", new ItemView("item-1002", "无线鼠标", new BigDecimal("28.50"), "seller-1002", true));
    }

    @Autowired
    public ItemService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ItemView getItem(String itemId) {
        if (jdbcTemplate != null) {
            return jdbcTemplate.query(
                    "SELECT id, title, price, seller_id, on_sale FROM items WHERE id = ?",
                    (resultSet, rowNum) -> new ItemView(
                            resultSet.getString("id"),
                            resultSet.getString("title"),
                            resultSet.getBigDecimal("price"),
                            resultSet.getString("seller_id"),
                            resultSet.getBoolean("on_sale")),
                    itemId).stream().findFirst().orElseThrow(() -> new ItemNotFoundException(itemId));
        }
        ItemView item = items.get(itemId);
        if (item == null) {
            throw new ItemNotFoundException(itemId);
        }
        return item;
    }

    public ItemView reserve(String itemId) {
        ItemView item = getItem(itemId);
        if (!item.onSale()) {
            throw new ItemUnavailableException(itemId);
        }
        ItemView reserved = item.withOnSale(false);
        if (jdbcTemplate != null) {
            int updated = jdbcTemplate.update(
                    "UPDATE items SET on_sale = FALSE WHERE id = ? AND on_sale = TRUE",
                    itemId);
            if (updated == 0) {
                throw new ItemUnavailableException(itemId);
            }
            return reserved;
        }
        items.put(itemId, reserved);
        return reserved;
    }

    public record ItemView(String id, String title, BigDecimal price, String sellerId, boolean onSale) {

        public ItemView withOnSale(boolean value) {
            return new ItemView(id, title, price, sellerId, value);
        }
    }

    public static class ItemNotFoundException extends RuntimeException {

        public ItemNotFoundException(String itemId) {
            super("商品不存在：" + itemId);
        }
    }

    public static class ItemUnavailableException extends RuntimeException {

        public ItemUnavailableException(String itemId) {
            super("商品当前不可购买：" + itemId);
        }
    }
}
