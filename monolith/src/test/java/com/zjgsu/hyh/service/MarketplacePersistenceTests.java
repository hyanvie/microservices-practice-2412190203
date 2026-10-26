package com.zjgsu.hyh.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.zjgsu.hyh.model.Item;
import com.zjgsu.hyh.model.ItemStatus;
import com.zjgsu.hyh.model.Order;
import com.zjgsu.hyh.model.OrderStatus;
import com.zjgsu.hyh.repository.ItemRepository;
import com.zjgsu.hyh.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MarketplacePersistenceTests {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemOrderService service;

    @Autowired
    private SampleDataInitializer sampleDataInitializer;

    @Test
    void sampleDataInitializationCanBeRepeated() {
        long itemCountBefore = itemRepository.count();

        sampleDataInitializer.run(new DefaultApplicationArguments());
        sampleDataInitializer.run(new DefaultApplicationArguments());

        assertEquals(itemCountBefore, itemRepository.count());
        assertNotNull(itemRepository.findById("item-1001").orElseThrow());
        assertNotNull(itemRepository.findById("item-1002").orElseThrow());
    }

    @Test
    void repositoriesPersistAndReadItemsAndOrders() {
        String itemId = UUID.randomUUID().toString();
        Item item = new Item(
                itemId,
                "Repository 测试商品",
                "验证商品 Repository 读写",
                new BigDecimal("23.50"),
                "教材",
                "GOOD",
                1,
                ItemStatus.ON_SALE,
                "seller-repository-test",
                Instant.now());
        itemRepository.saveAndFlush(item);

        String orderId = UUID.randomUUID().toString();
        Order order = new Order(
                orderId,
                item,
                "buyer-repository-test",
                item.sellerId(),
                item.price(),
                OrderStatus.PENDING_PAYMENT,
                Instant.now());
        orderRepository.saveAndFlush(order);

        Item persistedItem = itemRepository.findById(itemId).orElseThrow();
        Order persistedOrder = orderRepository.findById(orderId).orElseThrow();
        assertEquals("Repository 测试商品", persistedItem.title());
        assertEquals(itemId, persistedOrder.itemId());
        assertEquals("buyer-repository-test", persistedOrder.buyerId());
        assertEquals(new BigDecimal("23.50"), persistedOrder.price());
    }

    @Test
    void creatingOrderPersistsOrderAndSoldItemTogether() {
        String itemId = UUID.randomUUID().toString();
        itemRepository.saveAndFlush(new Item(
                itemId,
                "二手教材",
                "九成新",
                new BigDecimal("19.90"),
                "教材",
                "GOOD",
                1,
                ItemStatus.ON_SALE,
                "seller-persistence-test",
                Instant.now()));

        var order = service.createOrder(itemId, "buyer-persistence-test");

        var persistedOrder = orderRepository.findById(order.id()).orElseThrow();
        var persistedItem = itemRepository.findById(itemId).orElseThrow();
        assertEquals(itemId, persistedOrder.itemId());
        assertEquals(new BigDecimal("19.90"), persistedOrder.price());
        assertEquals(OrderStatus.PENDING_PAYMENT, persistedOrder.status());
        assertEquals(ItemStatus.SOLD, persistedItem.status());
        assertNotNull(persistedOrder.createdAt());
    }

    @Test
    void cancellingOrderPersistsCancelledStatusAndRestoresItem() {
        String itemId = UUID.randomUUID().toString();
        itemRepository.saveAndFlush(new Item(
                itemId,
                "闲置鼠标",
                "功能正常",
                new BigDecimal("12.00"),
                "数码",
                "GOOD",
                1,
                ItemStatus.ON_SALE,
                "seller-cancel-test",
                Instant.now()));
        var createdOrder = service.createOrder(itemId, "buyer-cancel-test");

        service.cancelOrder(createdOrder.id());

        assertEquals(
                OrderStatus.CANCELLED,
                orderRepository.findById(createdOrder.id()).orElseThrow().status());
        assertEquals(ItemStatus.ON_SALE, itemRepository.findById(itemId).orElseThrow().status());
    }
}
