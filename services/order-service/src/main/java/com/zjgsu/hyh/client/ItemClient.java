package com.zjgsu.hyh.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "item-service", path = "/api/items")
public interface ItemClient {

    @GetMapping("/{itemId}")
    ItemView getItem(@PathVariable("itemId") String itemId);
}
