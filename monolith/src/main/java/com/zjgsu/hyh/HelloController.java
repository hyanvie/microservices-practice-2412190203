package com.zjgsu.hyh;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    // GET /api/hello：返回项目名称，用于验证应用正常运行
    @GetMapping("/api/hello")
    public Map<String, String> hello() {
        return Map.of(
            "project", "拾光集 · 二手交易平台 (SecondLight)",
            "message", "Hello from SecondLight monolith!"
        );
    }
}