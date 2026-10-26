package com.zjgsu.hyh.service;

import com.zjgsu.hyh.model.Item;
import com.zjgsu.hyh.model.ItemStatus;
import com.zjgsu.hyh.repository.ItemRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SampleDataInitializer implements ApplicationRunner {

    private final ItemRepository itemRepository;

    public SampleDataInitializer(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        Instant now = Instant.now();
        if (!itemRepository.existsById("item-1001")) {
            itemRepository.save(new Item(
                    "item-1001",
                    "Java 编程教材",
                    "适合 Java 入门学习，保存完好。",
                    new BigDecimal("35.00"),
                    "教材",
                    "GOOD",
                    1,
                    ItemStatus.ON_SALE,
                    "seller-1001",
                    now));
        }
        if (!itemRepository.existsById("item-1002")) {
            itemRepository.save(new Item(
                    "item-1002",
                    "无线鼠标",
                    "可正常使用，附带接收器。",
                    new BigDecimal("28.50"),
                    "数码",
                    "LIKE_NEW",
                    1,
                    ItemStatus.ON_SALE,
                    "seller-1002",
                    now));
        }
    }
}
