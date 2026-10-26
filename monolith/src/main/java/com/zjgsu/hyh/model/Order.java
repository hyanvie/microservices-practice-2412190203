package com.zjgsu.hyh.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "buyer_id", nullable = false, length = 80)
    private String buyerId;

    @Column(name = "seller_id", nullable = false, length = 80)
    private String sellerId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Order() {
    }

    public Order(
            String id,
            Item item,
            String buyerId,
            String sellerId,
            BigDecimal price,
            OrderStatus status,
            Instant createdAt) {
        this.id = id;
        this.item = item;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.price = price;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String id() {
        return id;
    }

    public String itemId() {
        return item.getId();
    }

    @JsonIgnore
    public Item item() {
        return item;
    }

    public String buyerId() {
        return buyerId;
    }

    public String sellerId() {
        return sellerId;
    }

    public BigDecimal price() {
        return price;
    }

    public OrderStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Order withStatus(OrderStatus newStatus) {
        setStatus(newStatus);
        return this;
    }

    public String getId() {
        return id;
    }

    public String getItemId() {
        return item.getId();
    }

    public String getBuyerId() {
        return buyerId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
