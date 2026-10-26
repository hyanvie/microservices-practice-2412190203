package com.zjgsu.hyh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(name = "item_condition", nullable = false, length = 30)
    private String condition;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ItemStatus status;

    @Column(name = "seller_id", nullable = false, length = 80)
    private String sellerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Item() {
    }

    public Item(
            String id,
            String title,
            String description,
            BigDecimal price,
            String category,
            String condition,
            int quantity,
            ItemStatus status,
            String sellerId,
            Instant createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.category = category;
        this.condition = condition;
        this.quantity = quantity;
        this.status = status;
        this.sellerId = sellerId;
        this.createdAt = createdAt;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public BigDecimal price() {
        return price;
    }

    public String category() {
        return category;
    }

    public String condition() {
        return condition;
    }

    public int quantity() {
        return quantity;
    }

    public ItemStatus status() {
        return status;
    }

    public String sellerId() {
        return sellerId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    public Item withStatus(ItemStatus newStatus) {
        setStatus(newStatus);
        return this;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public String getCondition() {
        return condition;
    }

    public int getQuantity() {
        return quantity;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public String getSellerId() {
        return sellerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
