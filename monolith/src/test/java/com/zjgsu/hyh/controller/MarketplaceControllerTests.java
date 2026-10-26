package com.zjgsu.hyh.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zjgsu.hyh.model.Item;
import com.zjgsu.hyh.model.ItemStatus;
import com.zjgsu.hyh.model.Order;
import com.zjgsu.hyh.model.OrderStatus;
import com.zjgsu.hyh.service.BusinessRuleViolationException;
import com.zjgsu.hyh.service.ItemOrderService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class MarketplaceControllerTests {

    @Mock
    private ItemOrderService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MarketplaceController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void getItemReturnsItem() throws Exception {
        when(service.getItem("item-1001")).thenReturn(sampleItem());

        mockMvc.perform(get("/api/items/item-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Java 编程教材"))
                .andExpect(jsonPath("$.status").value("ON_SALE"));
    }

    @Test
    void createOrderReturnsCreated() throws Exception {
        when(service.createOrder("item-1001", "buyer-1001"))
                .thenReturn(new Order(
                        "order-1001",
                        sampleItem(),
                        "buyer-1001",
                        "seller-1001",
                        new BigDecimal("35.00"),
                        OrderStatus.PENDING_PAYMENT,
                        Instant.now()));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemId":"item-1001","buyerId":"buyer-1001"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/orders/order-1001"))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.itemId").value("item-1001"));
    }

    @Test
    void selfPurchaseReturnsConflict() throws Exception {
        when(service.createOrder("item-1001", "seller-1001"))
                .thenThrow(new BusinessRuleViolationException("不能购买自己发布的商品"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemId":"item-1001","buyerId":"seller-1001"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("不能购买自己发布的商品"));
    }

    private Item sampleItem() {
        return new Item(
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
}
