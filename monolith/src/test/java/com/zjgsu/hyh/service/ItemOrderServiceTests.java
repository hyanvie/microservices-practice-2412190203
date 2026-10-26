package com.zjgsu.hyh.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zjgsu.hyh.model.Item;
import com.zjgsu.hyh.model.ItemStatus;
import com.zjgsu.hyh.model.OrderStatus;
import com.zjgsu.hyh.repository.ItemRepository;
import com.zjgsu.hyh.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemOrderServiceTests {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private OrderRepository orderRepository;

    private ItemOrderService service;
    private Item item;

    @BeforeEach
    void setUp() {
        service = new ItemOrderService(itemRepository, orderRepository);
        item = new Item(
                "item-1001",
                "Java 编程教材",
                "适合 Java 入门学习，保存完好。",
                new BigDecimal("35.00"),
                "教材",
                "GOOD",
                1,
                ItemStatus.ON_SALE,
                "seller-1001",
                Instant.now());
    }

    @Test
    void createOrderMarksItemSoldAndCapturesCurrentPrice() {
        when(itemRepository.findByIdForUpdate("item-1001")).thenReturn(Optional.of(item));

        var order = service.createOrder("item-1001", "buyer-1001");

        assertEquals(OrderStatus.PENDING_PAYMENT, order.status());
        assertEquals("35.00", order.price().toPlainString());
        assertEquals(ItemStatus.SOLD, item.status());
        verify(orderRepository).save(order);
        verify(itemRepository).save(item);
    }

    @Test
    void sellerCannotBuyOwnItem() {
        when(itemRepository.findByIdForUpdate("item-1001")).thenReturn(Optional.of(item));

        assertThrows(
                BusinessRuleViolationException.class,
                () -> service.createOrder("item-1001", "seller-1001"));

        assertEquals(ItemStatus.ON_SALE, item.status());
    }

    @Test
    void soldItemCannotBeOrderedAgain() {
        item.setStatus(ItemStatus.SOLD);
        when(itemRepository.findByIdForUpdate("item-1001")).thenReturn(Optional.of(item));

        assertThrows(
                BusinessRuleViolationException.class,
                () -> service.createOrder("item-1001", "buyer-1001"));

        assertEquals(ItemStatus.SOLD, item.status());
    }

    @Test
    void cancellingOrderRestoresItemForSale() {
        when(itemRepository.findByIdForUpdate("item-1001")).thenReturn(Optional.of(item));
        var order = service.createOrder("item-1001", "buyer-1001");
        when(orderRepository.findByIdForUpdate(order.id())).thenReturn(Optional.of(order));
        when(itemRepository.findByIdForUpdate("item-1001")).thenReturn(Optional.of(item));

        var cancelledOrder = service.cancelOrder(order.id());

        assertEquals(OrderStatus.CANCELLED, cancelledOrder.status());
        assertEquals(ItemStatus.ON_SALE, item.status());
        verify(itemRepository, times(2)).save(item);
    }
}
