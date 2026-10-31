package com.zjgsu.hyh.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class ItemService {

    private final Map<String, ItemView> items = new ConcurrentHashMap<>();

    public ItemService() {
        items.put("item-1001", new ItemView("item-1001", "Java 编程教材", new BigDecimal("35.00"), "seller-1001", true));
        items.put("item-1002", new ItemView("item-1002", "无线鼠标", new BigDecimal("28.50"), "seller-1002", true));
    }

    public ItemView getItem(String itemId) {
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
