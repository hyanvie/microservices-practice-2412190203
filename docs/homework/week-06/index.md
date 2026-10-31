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

三个服务的本地编排和跨服务烟测从仓库根目录运行：

```bash
./scripts/smoke-test.sh
```

脚本会构建并启动三个服务，检查三个 Actuator 健康接口，再通过
`order-service -> item-service` 的真实 HTTP 调用创建一笔订单，最后自动停止
本次启动的容器。

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

当前代码的商品、用户和订单数据仍由各服务进程内存保存，尚未接入独立数据库
和真实数据表。因此“独立数据归属”已在设计和接口边界中完成，但数据库落地
仍是本小节未完成项，不能用当前实现声称该评分点已全部通过。

### 运行和验证

在仓库根目录运行每个服务的测试：

```bash
./monolith/mvnw -B -f services/user-service/pom.xml test
./monolith/mvnw -B -f services/item-service/pom.xml test
./monolith/mvnw -B -f services/order-service/pom.xml test
```

三个服务测试均通过，结果为 `BUILD SUCCESS`。其中订单服务测试覆盖了商品
服务校验成功后创建订单，以及商品服务不可用时不返回成功订单。

也可以运行完整烟测：

```bash
./scripts/smoke-test.sh
```

截图待补：请按仓库根目录运行烟测后，截取终端中三个健康检查和
`cross-service order smoke test passed` 的完整输出，保存到
`docs/homework/week-06/screenshots/02-smoke-test-success.png`。
