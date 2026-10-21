package com.zjgsu.hyh;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class HelloControllerTests {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // 单独加载 HelloController，不启动整个 Spring 容器，速度快
        mockMvc = MockMvcBuilders.standaloneSetup(new HelloController()).build();
    }

    @Test
    void helloShouldReturnProjectInfo() throws Exception {
        // 请求 GET /api/hello，期望状态码 200，并且 JSON 里带有 project 和 message 字段
        mockMvc.perform(get("/api/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project").exists())
                .andExpect(jsonPath("$.message").exists());
    }
}