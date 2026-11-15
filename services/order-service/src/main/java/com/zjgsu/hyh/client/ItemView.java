package com.zjgsu.hyh.client;

import java.math.BigDecimal;

public record ItemView(String id, String title, BigDecimal price, String sellerId, boolean onSale) {
}
