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

服务边界设计已完成，三个独立工程也已创建，并补充了基础 HTTP 协作。

### 设计文档检查

```bash
test -f docs/microservices-design.md
grep -E "item-service|order-service|user-service" docs/microservices-design.md
```

后续服务工程完成后，统一从仓库根目录运行：

```bash
./scripts/smoke-test.sh
```

每个服务的测试命令会在 README 和本文件中记录。

## 2. 代码拆分

### 实现思路

在 `services/` 下创建三个独立 Spring Boot 工程：

- `user-service`：端口 `8083`，提供用户 ID 校验接口。
- `item-service`：端口 `8081`，提供商品查询和商品预留接口。
- `order-service`：端口 `8082`，提供订单创建和查询接口。

三个工程都使用 `com.zjgsu.hyh` 包名，分别拥有自己的 `pom.xml`、配置文件、
Controller、Service 和测试。订单服务在创建订单前通过 `RestTemplate` 请求
`ITEM_SERVICE_URL/api/items/{itemId}` 查询商品。商品不存在返回冲突错误，
商品服务连接失败返回 `503 Service Unavailable`，校验失败时不会写入订单。
服务地址、连接超时和读取超时都可以通过环境变量覆盖。

### 运行和验证

在仓库根目录运行每个服务的测试：

```bash
./monolith/mvnw -B -f services/user-service/pom.xml test
./monolith/mvnw -B -f services/item-service/pom.xml test
./monolith/mvnw -B -f services/order-service/pom.xml test
```

三个服务测试均通过，结果为 `BUILD SUCCESS`。其中订单服务测试覆盖了商品
服务校验成功后创建订单，以及商品服务不可用时不返回成功订单。
