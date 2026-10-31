# 第六周：微服务拆分

## 1. 服务拆分设计

本周先保留 `monolith/` 单体工程，新增 `services/` 目录承载独立微服务。根据拾光集当前的商品和订单业务，将系统拆分为 `item-service`、`order-service` 和 `user-service` 三个服务。

每个服务有自己的 Spring Boot 工程和数据库，只访问自己拥有的表。订单服务需要商品和用户信息时，通过 HTTP 调用对应服务，不直接访问其他服务的 Repository。详细的边界、端口、数据库、表归属和 Mermaid 架构图见 [`docs/microservices-design.md`](../microservices-design.md)。

### 关键结论

- `item-service` 负责商品、库存和商品状态，端口 `8081`。
- `order-service` 负责订单状态和下单流程，端口 `8082`。
- `user-service` 负责用户基础信息和身份校验，端口 `8083`。
- 三个服务分别使用 `secondlight_item`、`secondlight_order`、`secondlight_user` 数据库。
- 订单只保存 `itemId`、`buyerId`、`sellerId`，不建立跨库外键。

### 当前完成情况

本项已完成服务边界设计文档。下一步创建三个独立的 Maven/Spring Boot 工程，实现各自健康接口和基础 HTTP 协作。

### 设计文档检查

```bash
test -f docs/microservices-design.md
grep -E "item-service|order-service|user-service" docs/microservices-design.md
```

后续服务工程完成后，统一从仓库根目录运行：

```bash
./scripts/smoke-test.sh
```

每个服务的测试命令会在服务实现完成后补充到 README 和本文件。
