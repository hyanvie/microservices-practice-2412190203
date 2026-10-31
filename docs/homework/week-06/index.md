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

服务边界设计已完成：已完成单体模块映射、三个服务的职责与数据归属、服务调用
关系和 Mermaid 架构图；三个服务也已分别接入各自的 MySQL 数据库和数据表。

### 设计文档检查

```bash
test -f docs/microservices-design.md
grep -E "item-service|order-service|user-service" docs/microservices-design.md
```

上述命令用于核对设计文档存在且包含三个服务；文档中的设计验收清单和架构图
同时作为本小节的 GitHub 交付证据。

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

当前代码已为三个服务分别接入独立 MySQL 数据库和数据表；数据库连接通过环境
变量配置，服务启动时初始化本服务拥有的表。跨服务关联仍只保存业务 ID，不
跨库读写数据表。

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

验证截图已保存到 `screenshots/`：

- `01-user-services-test-success.png`：user-service 测试通过。
- `02-item-services-test-success.png`：item-service 测试通过。
- `03-order-services-test-success.png`：order-service 测试通过。
- `04-smoke-test-success.png`：三个健康检查和跨服务下单烟测通过。

## 3. 本地编排

### 实现思路

根目录 `compose.yaml` 编排三个 Spring Boot 服务和三个独立的 MySQL 容器：
`user-service` 使用 `secondlight_user` 和 `user_user`，`item-service` 使用
`secondlight_item` 和 `item_user`，`order-service` 使用 `secondlight_order` 和
`order_user`。每个数据库只由对应服务使用，服务通过 Compose 服务名访问数据库
和其他服务，不使用容器内的 `localhost`。三个数据库分别挂载命名数据卷，
服务启动前等待对应数据库健康检查通过。

每个服务的 `schema.sql` 只创建和初始化自己的表：`users`、`items` 或
`orders`。订单中的 `item_id`、`buyer_id` 和 `seller_id` 仅保存跨服务业务 ID，
不建立跨数据库外键。

### 运行和验证

在仓库根目录执行：

```bash
docker compose up -d --build
docker compose ps
./scripts/smoke-test.sh
```

预期三个数据库显示 `healthy`，三个服务健康检查通过，并看到
`cross-service order smoke test passed`。数据卷验证可使用：

```bash
docker volume ls | grep microservices-practice-2412190203
```

本地编排验证截图应保存为 `screenshots/05-compose-database-smoke-success.png`，
内容应包含三个数据库健康检查、三个服务健康检查和跨服务下单烟测结果。
