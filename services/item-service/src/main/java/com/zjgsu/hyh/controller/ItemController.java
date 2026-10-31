package com.zjgsu.hyh.controller;

import com.zjgsu.hyh.service.ItemService;
import com.zjgsu.hyh.service.ItemService.ItemView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping("/{itemId}")
    public ItemView getItem(@PathVariable String itemId) {
        return itemService.getItem(itemId);
    }

    @PostMapping("/{itemId}/reserve")
    public ItemView reserve(@PathVariable String itemId) {
        return itemService.reserve(itemId);
    }
}
