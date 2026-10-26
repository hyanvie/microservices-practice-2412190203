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

## 单体应用实现

### 实现思路

- `model`：JPA 实体 `Item` 与 `Order`，订单通过外键关联商品。
- `repository`：Spring Data JPA Repository 负责数据库读写。
- `service`：实现查询、创建/取消订单、业务规则校验和事务控制。
- `controller`：提供 `/api` 路径 REST 接口，只负责 HTTP 输入输出。
- 应用默认使用 H2 文件数据库，数据存放在 `monolith/data/secondlight`；首次启动会写入两个演示商品。测试配置使用内存 H2。

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
