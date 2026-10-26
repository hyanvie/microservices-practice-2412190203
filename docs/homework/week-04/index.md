# Week 04：单体应用与数据库持久化

## 本周目标

在第三周创建的 `monolith/` Spring Boot 工程中完成拾光集“买家购买在售商品”流程，包括业务建模、商品与订单模型、Service 业务规则、REST API 和数据库持久化。

## 业务建模

### 实现思路与关键结论

核心流程是买家下单购买一件在售商品，参与实体为商品（Item）和订单（Order）。一件商品可关联多条历史订单，但库存为 1 的商品同一时间只能有一个有效订单。订单保存商品、买家、卖家、下单价格和状态；取消待支付订单后保留订单记录并恢复商品在售状态。

Service 层强制检查商品必须处于在售状态，且买家不能是卖家本人。创建订单与商品转为已售出、取消订单与商品恢复在售分别放在同一个事务中；下单时对商品行加写锁，避免并发请求重复购买。实体字段及必要约束记录在 [`docs/database.md`](../../database.md)，模型和规则来源见 [`docs/project-proposal.md`](../../project-proposal.md)。

**完成结果：** 两个实体及关系、完整下单/取消流程和 Service 业务规则均已实现；不符合规则的下单返回 `409 Conflict` 且不会生成订单。

### 验证

```bash
cd monolith
./mvnw test
```

测试通过（`BUILD SUCCESS`）。测试使用独立的内存 H2 数据库，不依赖运行中的服务。

## 数据库持久化

### 实现思路与完成结果

应用默认数据源已接入本地 MySQL 8.0+，通过 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 配置连接；Hibernate 根据 JPA 实体维护 `items` 和 `orders` 两张业务表。`Order` 通过非空外键关联 `Item`，必填字段、主键、金额精度和字段长度约束均在实体映射中定义。数据访问由 `repository` 包中的 `ItemRepository`、`OrderRepository` 提供，创建订单 API 使用 `OrderRequest` DTO 接收输入。

`SampleDataInitializer` 在应用启动时按商品主键补齐演示数据，重复启动不会重复插入。自动化测试仍使用独立的内存 H2，覆盖订单与商品状态持久化及演示数据初始化的幂等性。

### 本地运行与验证

先按仓库根目录 README 的步骤安装并启动 MySQL，创建 `secondlight` 数据库和应用账号，在终端设置 `DB_USERNAME`、`DB_PASSWORD`（可选设置 `DB_URL`），然后在 `monolith/` 下执行：

```bash
./mvnw test
./mvnw spring-boot:run
```

应用启动后，在另一终端验证数据库表和演示数据：

```bash
mysql -u secondlight -p -D secondlight -e "SHOW TABLES; SELECT id,title,status FROM items;"
curl -i http://localhost:8080/api/items
```

创建订单并检查其数据库记录及商品状态变化：

```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"itemId":"item-1001","buyerId":"buyer-2001"}'
mysql -u secondlight -p -D secondlight -e "SELECT id,item_id,buyer_id,status FROM orders; SELECT id,status FROM items WHERE id='item-1001';"
```

预期 `./mvnw test` 显示 `BUILD SUCCESS`；MySQL 中可看到 `items`、`orders` 两张表，成功下单后 `orders` 有记录且 `item-1001` 状态为 `SOLD`。重启应用后再次查询，订单和商品状态仍保留。

### 截图

请将实际运行截图保存到 `screenshots/`：

- `04-mysql-tables-and-data.png`：MySQL `SHOW TABLES` 及演示商品查询结果
- `05-mysql-order-persistence.png`：创建订单的 API 响应及 MySQL 订单/商品状态查询结果
- `06-mysql-tests.png`：`./mvnw test` 的 `BUILD SUCCESS` 结果

## 测试与验证

### 实现思路与覆盖范围

测试按 Repository、Service、API 三层分别验证，并使用 `test` profile 的内存 H2 数据库，不依赖本地 MySQL：

- **Repository 读写：** `MarketplacePersistenceTests.repositoriesPersistAndReadItemsAndOrders` 通过 `ItemRepository`、`OrderRepository` 保存并重新读取商品和订单，校验关联、买家及金额字段；测试事务结束后自动回滚，重复执行不会污染数据。
- **Service 业务规则：** `ItemOrderServiceTests` 验证正常下单会记录价格并售出商品，拒绝卖家自购和重复购买已售商品，以及取消订单后恢复商品在售。
- **API 正常与错误响应：** `MarketplaceControllerTests` 验证商品查询返回 `200`、成功创建订单返回 `201`，以及卖家自购返回 `409` 和错误详情。
- **测试数据重复运行：** `MarketplacePersistenceTests.sampleDataInitializationCanBeRepeated` 连续执行演示数据初始化并验证记录数不变。

### 运行命令与结果

在仓库根目录运行完整测试套件：

```bash
cd monolith
./mvnw test
```

预期 Maven 显示 `BUILD SUCCESS`，所有测试 `Failures: 0, Errors: 0`。可重复执行同一命令；测试使用每次重建的内存 H2 数据库，演示数据初始化本身也有幂等测试。

### 截图

请将运行 `./mvnw test` 并显示各测试类结果及最终 `BUILD SUCCESS` 的终端截图保存为 `screenshots/07-test-suite-success.png`。

## 单体应用实现

### 实现思路

- `model`：JPA 实体 `Item` 与 `Order`，订单通过外键关联商品。
- `repository`：Spring Data JPA Repository 负责数据库读写。
- `service`：实现查询、创建/取消订单、业务规则校验和事务控制。
- `controller`：提供 `/api` 路径 REST 接口，只负责 HTTP 输入输出。
- 应用使用本地 MySQL 持久化；首次启动会补齐两个演示商品。自动化测试配置使用内存 H2。

接口包括：

| 方法 | 路径 | 用途 |
|---|---|---|
| `GET` | `/api/items` | 查询商品列表 |
| `GET` | `/api/items/{itemId}` | 查询商品详情 |
| `POST` | `/api/orders` | 创建订单 |
| `GET` | `/api/orders` | 查询订单，可用 `buyerId` 筛选 |
| `GET` | `/api/orders/{orderId}` | 查询订单详情 |
| `POST` | `/api/orders/{orderId}/cancel` | 取消待支付订单 |

### 运行命令

在仓库根目录执行：

```bash
cd monolith
./mvnw test
./mvnw spring-boot:run
```

应用启动后，可在另一个终端验证商品查询：

```bash
curl -i http://localhost:8080/api/items/item-1001
```

创建订单（首次运行时商品 `item-1001` 在售；成功后会变为已售出）：

```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"itemId":"item-1001","buyerId":"buyer-1001"}'
```

卖家自购验证（使用仍在售的 `item-1002` 和其卖家 `seller-1002`，预期 `409 Conflict`）：

```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"itemId":"item-1002","buyerId":"seller-1002"}'
```

### 验证结果与截图

自动化测试覆盖 Service 业务规则、Controller HTTP 状态码和 JPA 数据持久化/状态更新。截图展示接口查询、成功创建订单及拒绝卖家自购：

- [商品详情查询](screenshots/01-get-item.png)
- [创建订单](screenshots/02-create-order.png)
- [拒绝卖家自购](screenshots/03-reject-self-purchase.png)

## 完成情况

- [x] 更新项目提案，说明核心流程、实体关系和 Service 业务规则
- [x] 完成 Item、Order JPA 实体和数据库关联约束
- [x] 完成 Service、REST API 和原子化下单/取消流程
- [x] 增加可重复运行的自动化测试及接口截图
- [x] README 补充启动命令和可运行的 curl 示例
- [x] 接入本地 MySQL，补充表结构、连接配置和重复运行安全的演示数据初始化
- [x] 增加 Repository 读写、Service 规则、API 成功/错误响应及可重复初始化测试
