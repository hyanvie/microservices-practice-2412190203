package com.zjgsu.hyh.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ItemServiceTest {

    @Test
    void reserveMarksItemUnavailable() {
        ItemService service = new ItemService();

        service.reserve("item-1001");

        assertFalse(service.getItem("item-1001").onSale());
        assertThrows(ItemService.ItemUnavailableException.class, () -> service.reserve("item-1001"));
    }
}
