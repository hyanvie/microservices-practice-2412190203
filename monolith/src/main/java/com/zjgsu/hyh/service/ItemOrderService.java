package com.zjgsu.hyh.service;

import com.zjgsu.hyh.model.Item;
import com.zjgsu.hyh.model.ItemStatus;
import com.zjgsu.hyh.model.Order;
import com.zjgsu.hyh.model.OrderStatus;
import com.zjgsu.hyh.repository.ItemRepository;
import com.zjgsu.hyh.repository.OrderRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ItemOrderService {

    private final ItemRepository itemRepository;
    private final OrderRepository orderRepository;

    public ItemOrderService(ItemRepository itemRepository, OrderRepository orderRepository) {
        this.itemRepository = itemRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<Item> getItems() {
        return itemRepository.findAllByOrderByCreatedAtAsc();
    }

    @Transactional(readOnly = true)
    public Item getItem(String itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在：" + itemId));
    }

    @Transactional
    public Order createOrder(String itemId, String buyerId) {
        if (!StringUtils.hasText(itemId) || !StringUtils.hasText(buyerId)) {
            throw new IllegalArgumentException("itemId 和 buyerId 不能为空");
        }

        Item item = itemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在：" + itemId));
        if (item.status() != ItemStatus.ON_SALE) {
            throw new BusinessRuleViolationException("商品当前不在售，无法下单");
        }
        if (item.sellerId().equals(buyerId)) {
            throw new BusinessRuleViolationException("不能购买自己发布的商品");
        }

        Order order = new Order(
                UUID.randomUUID().toString(),
                item,
                buyerId,
                item.sellerId(),
                item.price(),
                OrderStatus.PENDING_PAYMENT,
                Instant.now());
        item.setStatus(ItemStatus.SOLD);
        orderRepository.save(order);
        itemRepository.save(item);
        return order;
    }

    @Transactional(readOnly = true)
    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在：" + orderId));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrders(String buyerId) {
        if (buyerId == null) {
            return orderRepository.findAllByOrderByCreatedAtAsc();
        }
        if (!StringUtils.hasText(buyerId)) {
            throw new IllegalArgumentException("buyerId 不能为空");
        }
        return orderRepository.findByBuyerIdOrderByCreatedAtAsc(buyerId);
    }

    @Transactional
    public Order cancelOrder(String orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在：" + orderId));
        if (order.status() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessRuleViolationException("只有待支付订单可以取消");
        }
        Item item = itemRepository.findByIdForUpdate(order.itemId())
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在：" + order.itemId()));
        order.setStatus(OrderStatus.CANCELLED);
        item.setStatus(ItemStatus.ON_SALE);
        itemRepository.save(item);
        return order;
    }
}
