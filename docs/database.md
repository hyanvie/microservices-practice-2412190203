# 拾光集数据库设计

第四周使用 Spring Data JPA 和 MySQL 持久化商品与订单。应用启动时由 Hibernate 根据实体创建或更新表结构；自动化测试使用独立的内存 H2 数据库，不连接本地 MySQL。

## `items` 商品表

| 列 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | `VARCHAR(36)` | 主键，不可空 | 商品编号 |
| `title` | `VARCHAR(120)` | 不可空 | 商品标题 |
| `description` | `VARCHAR(2000)` | 可空 | 商品描述 |
| `price` | `DECIMAL(12,2)` | 不可空 | 商品价格 |
| `category` | `VARCHAR(60)` | 不可空 | 商品分类 |
| `item_condition` | `VARCHAR(30)` | 不可空 | 商品成色 |
| `quantity` | `INTEGER` | 不可空 | 商品数量 |
| `status` | `VARCHAR(30)` | 不可空 | 商品状态，按枚举名称保存 |
| `seller_id` | `VARCHAR(80)` | 不可空 | 卖家编号 |
| `created_at` | `TIMESTAMP` | 不可空 | 发布时间 |

## `orders` 订单表

| 列 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | `VARCHAR(36)` | 主键，不可空 | 订单编号 |
| `item_id` | `VARCHAR(36)` | 外键至 `items.id`，不可空 | 订单对应商品 |
| `buyer_id` | `VARCHAR(80)` | 不可空 | 买家编号 |
| `seller_id` | `VARCHAR(80)` | 不可空 | 下单时记录的卖家编号 |
| `price` | `DECIMAL(12,2)` | 不可空 | 下单时价格快照 |
| `status` | `VARCHAR(30)` | 不可空 | 订单状态，按枚举名称保存 |
| `created_at` | `TIMESTAMP` | 不可空 | 下单时间 |

## 关系、约束与一致性

- 一个商品可关联多条历史订单；每条订单必须关联一个商品。
- 商品与订单编号使用主键，`orders.item_id` 使用非空外键，阻止订单引用不存在的商品。
- 两表的业务必填字段均设置 `NOT NULL`；金额用 `DECIMAL(12,2)`，商品标题、描述、分类、状态和用户编号均设置最大长度。
- 金额采用定点数并保留两位小数；状态使用字符串枚举，避免依赖枚举序号。
- 当前不单独建立用户表，买卖双方以编号记录，符合本周范围。
- 下单通过事务和商品行写锁检查商品仍在售，再写入订单并将商品标记为已售；取消待支付订单也在同一事务中更新订单和商品。
- 本地运行使用 MySQL 8.0 或更新版本，连接地址、用户名和密码分别由 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 提供；未设置 URL 时连接 `localhost:3306/secondlight`。
- Hibernate 使用 `ddl-auto: update` 便于课程实践；正式部署应改用显式数据库迁移工具管理结构版本。
- `SampleDataInitializer` 按演示商品主键逐条检查，只补齐缺少的数据；重复启动不会重复插入 `item-1001` 和 `item-1002`。
- `OrderRequest` 是创建订单的 API 输入 DTO；数据访问统一经 `repository` 包中的 Spring Data JPA Repository 完成。
