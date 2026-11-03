# 第七周：Nacos 服务注册与发现

## 作业范围

本周在第六周已有的 `item-service`、`order-service` 和 `user-service` 中接入
Nacos Discovery，并在根目录 `compose.yaml` 中增加 Nacos 3.2.4 编排。
保留原有服务名、数据归属、接口和业务规则；第一小节完成服务注册配置，
第二小节将订单创建前的商品校验改为通过服务发现调用。

## Nacos 部署与初始化

- 镜像：`nacos/nacos-server:v3.2.4`
- 模式：standalone
- 控制台：`http://localhost:18081`
- 服务端：`localhost:18848`
- SDK 通信：`localhost:19848`
- 数据卷：`nacos-data`、`nacos-logs`
- 鉴权：启用 Token、Server identity；真实值仅保存在本地 `.env`

启动顺序：

```bash
docker compose up -d nacos
# Nacos 3.2 首次没有默认管理员密码，先初始化 nacos 管理员
curl -X POST http://localhost:18848/nacos/v3/auth/user/admin \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'password=replace-with-a-local-password'
# 打开 http://localhost:18081，用 nacos 和上面的密码登录控制台
# 将修改后的 NACOS_USERNAME/NACOS_PASSWORD 写回本地 .env
docker compose up -d user-db item-db order-db user-service item-service order-service
docker compose ps
```

## 服务发现调用

### 调用方向与隔离配置

本项目选择“下单前校验商品”流程：

```text
order-service --DiscoveryClient--> item-service
```

三个服务使用默认命名空间 `public` 和统一分组 `DEFAULT_GROUP`。服务端地址、
用户名和密码均通过环境变量注入；Compose 网络内使用 `NACOS_SERVER_ADDR=nacos:8848`，
不把 Nacos 控制台或服务端地址写入业务调用代码。`order-service` 的
`NACOS_DISCOVERY_ENABLED` 仅在 Compose 中设为 `true`，单元测试默认关闭真实
注册中心连接。

### 发现实现与错误语义

`order-service` 每次创建订单时调用：

```java
discoveryClient.getInstances("item-service")
```

从当前实例列表选择一个实例，并将其 URI 与原业务路径
`/api/items/{itemId}` 组合后调用。没有实例时返回 `503 Service Unavailable`；
商品不存在仍保留原有 `409 Conflict` 业务响应；连接和读取超时继续由
`ITEM_CONNECT_TIMEOUT_MS`、`ITEM_READ_TIMEOUT_MS` 控制。原固定地址
`ITEM_SERVICE_URL` 已从配置和 Compose 中删除。

### 测试与请求验证

订单服务测试使用模拟的 `DiscoveryClient`，覆盖：

- 发现 `item-service` 实例后使用实例 URI 请求商品并创建订单；
- 商品服务请求失败时不保存订单；
- 实例列表为空时返回 `DependencyUnavailableException`，不保存订单。

从仓库根目录运行：

```bash
./monolith/mvnw -B -f services/order-service/pom.xml test
```

Compose 启动后，可使用宿主机端口验证真实业务请求：

```bash
curl -i http://localhost:18091/api/items/item-1001
curl -i -X POST http://localhost:18092/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"itemId":"item-1001","buyerId":"buyer-1001"}'
```

预期商品请求返回 `200`，创建订单返回 `201`。停止 `item-service` 后再次
创建订单应返回 `503`；恢复服务并等待重新注册后，创建订单恢复正常。

第二小节截图应保存为：

- `screenshots/04-discovery-instance-and-order-success.png`
- `screenshots/05-discovery-empty-and-service-recovery.png`

## 健康状态、服务下线与恢复

三个业务服务均保留 Actuator 健康端点，分别通过宿主机端口访问：

```bash
curl -i http://localhost:18093/actuator/health
curl -i http://localhost:18091/actuator/health
curl -i http://localhost:18092/actuator/health
```

服务正常时返回 HTTP `200`，响应中的 `status` 为 `UP`。第三小节使用与第二
小节相同的调用方向 `order-service -> item-service`，验证步骤如下：

```bash
# 1. 依赖正常：商品校验成功并创建订单
curl -i -X POST http://localhost:18092/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"itemId":"item-1001","buyerId":"buyer-health-1001"}'

# 2. 停止被调用服务，等待 Nacos 实例列表移除 item-service
docker compose stop item-service
sleep 30
curl -i -X POST http://localhost:18092/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"itemId":"item-1002","buyerId":"buyer-health-1002"}'

# 3. 恢复服务，等待健康检查和重新注册
docker compose start item-service
curl -i http://localhost:18091/actuator/health
sleep 30
curl -i -X POST http://localhost:18092/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"itemId":"item-1002","buyerId":"buyer-health-1003"}'
```

实际结果记录：

- 依赖正常时，订单创建返回 HTTP `201`，订单服务的 `orders` 表新增一条
  `PENDING_PAYMENT` 记录。
- `item-service` 停止并从 Nacos 实例列表消失后，订单请求返回 HTTP `503`，
  错误为“商品服务当前没有可用实例”；商品校验失败发生在订单插入之前，
  `orders` 表记录数保持不变。
- `item-service` 恢复后，`/actuator/health` 返回 HTTP `200` 且状态为 `UP`，
  Nacos 重新显示实例，订单请求恢复返回 HTTP `201`。

可用以下命令核对失败请求没有新增本地订单记录：

```bash
docker compose exec -T order-db \
  mysql -uorder_user -porder_pass -Nse 'SELECT COUNT(*) FROM orders'
```

健康状态与注册状态不是同一个概念：

- **SDK 连接存活**表示业务服务中的 Nacos 客户端仍能与 Nacos 通信，能够
  发送心跳、获取服务实例或执行注册操作。
- **Nacos 实例状态**表示 Nacos 根据实例心跳和临时实例机制判断该服务实例
  是否仍在注册列表中；服务停止后通常需要等待心跳超时，列表才会变化。
- **Actuator 应用健康**表示 Spring Boot 应用及其健康检查组件当前是否可用，
  例如返回 `UP` 或 `DOWN`。Actuator 返回 `DOWN` 不会自动保证 Nacos 立即
  摘除实例，除非另外配置健康状态联动或服务主动注销。

本验证没有破坏数据库或模拟网络分区；服务发现只负责定位实例，不能消除
跨服务写操作的部分成功事务窗口，本项目也未实现自动补偿或分布式事务。

第三小节截图建议保存为：

- `screenshots/06-actuator-health-up.png`
- `screenshots/07-service-down-failure-and-recovery.png`

## 注册结果记录

启动业务服务后，在 Nacos 控制台的 `DEFAULT_GROUP` 中确认以下服务和实例：

| 服务名 | 宿主机端口 | 预期实例端口 |
|---|---:|---:|
| `user-service` | 18093 | 8083 |
| `item-service` | 18091 | 8081 |
| `order-service` | 18092 | 8082 |

截图保存到 `screenshots/` 后，在下表补充实际截图文件名和控制台显示的实例
IP。截图应隐藏 Token、密码等凭据。

| 内容 | 截图 |
|---|---|
| Nacos 控制台服务列表、分组和三个实例 | `screenshots/01-nacos-console-service-list.png` |
| 三个业务服务健康检查 | `screenshots/02-compose-services-health.png` |
| 三个服务注册日志 | `screenshots/03-nacos-registration-logs.png` |

## 本地测试

服务单测不连接真实 Nacos 或 MySQL，分别从仓库根目录执行：

```bash
./monolith/mvnw -B -f services/user-service/pom.xml test
./monolith/mvnw -B -f services/item-service/pom.xml test
./monolith/mvnw -B -f services/order-service/pom.xml test
```

Compose 烟测从仓库根目录执行：

```bash
bash scripts/smoke-test.sh
```
