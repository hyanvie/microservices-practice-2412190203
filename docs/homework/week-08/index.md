# 第八周：服务通信与负载均衡

## 选择一个跨服务业务流程

本周实现二手交易平台的“创建订单前校验商品”流程。调用方是 `order-service`，被调用方是 `item-service`。客户端请求 `POST /api/orders` 后，订单服务通过 OpenFeign 调用商品服务的 `GET /api/items/{itemId}`，确认商品存在且在售，再把商品 ID、买家 ID、卖家 ID 和当时的价格写入订单服务自己的数据库。订单服务不直接访问商品数据库，也不调用课堂中的选课服务或复用选课业务规则。

`item-service` 通过 Nacos 以 `item-service` 服务名注册；Feign 使用服务名而非固定 IP，Spring Cloud LoadBalancer 从健康实例中选择目标。错误商品映射为 HTTP `409`，商品服务不可用映射为 HTTP `503`；只有商品校验通过才写入订单。该流程是先查询再创建的同步协作，不构成跨服务事务，也不包含商品锁定或库存预留。

调用链路图和一致性边界见 [服务间同步调用与负载均衡](../../service-communication.md)。

工作区现有进度 PPT 第 5 页展示的服务通信结果是依赖服务下线返回 `503`、恢复后创建订单返回 `201`。本作业沿用这两个状态码验证服务故障恢复；商品不存在或不可购买时的 `409` 是另一种业务错误，不替代依赖不可用的 `503`。

## 实现思路

1. 在 `order-service` 声明 OpenFeign `ItemClient`，以 `item-service` 为服务名访问 `GET /api/items/{itemId}`。
2. 通过 Nacos 服务发现和 Spring Cloud LoadBalancer 在多个商品服务实例间分配请求；连接和读取超时沿用 `ITEM_CONNECT_TIMEOUT_MS`、`ITEM_READ_TIMEOUT_MS`。
3. 把 Feign 的 `404` 映射为商品业务冲突，把其他 Feign 远程调用异常映射为依赖不可用；失败请求不写入订单。
4. 在根 `compose.yaml` 中允许扩展商品服务副本，并让 `scripts/e2e-test.sh` 使用两个实例验证真实请求分配。

## 运行命令

单元测试（从仓库根目录运行）：

```bash
bash monolith/mvnw -B -f services/order-service/pom.xml test
```

端到端测试（需 Docker Compose、已配置的本地 `.env` 和可用的 Nacos 凭据）：

```bash
bash scripts/e2e-test.sh
```

脚本会构建并启动服务，验证商品服务两个实例健康、成功创建订单的 JSON 响应、错误商品的 `409` 与订单数据不变，并检查两个实例都实际收到 Feign 请求；随后停止两个商品服务实例，验证不可用时返回 `503` 且未落订单，恢复服务后再验证 `201`。脚本停止并移除本次启动的容器和网络，但保留命名数据卷。

## 运行结果与截图

- 订单服务单元测试：`bash monolith/mvnw -B -f services/order-service/pom.xml test`，4 项测试通过。
- 商品服务回归测试：`bash monolith/mvnw -B -f services/item-service/pom.xml test`，1 项测试通过。
- Bash 语法、`docker compose config --quiet` 和 `git diff --check` 检查通过。
- Compose 端到端与双实例负载均衡及故障恢复：本环境的 WSL 未安装/接入 Docker CLI，`bash scripts/e2e-test.sh` 在前置检查处退出，没有启动或停止容器；脚本包含进度 PPT 中的 `503` 下线和 `201` 恢复检查，仍需在 Docker 可用的环境实际运行。
- 截图尚未生成。Docker 环境验证通过后，将脚本输出及两个商品服务实例截图保存至 `screenshots/01-e2e-load-balancing.png`；截图中不得出现 Nacos 密码、Token 或其他凭据。

## 使用 OpenFeign

### 实现思路

调用方为 `order-service`，被调用方为 `item-service`。订单服务中的 `ItemClient` 使用 `@FeignClient(name = "item-service", path = "/api/items")` 按 Nacos 服务名声明调用契约，通过 `GET /{itemId}` 查询商品；Spring Cloud LoadBalancer 根据服务发现结果选择实例，不在客户端写死容器 IP。OpenFeign 连接和读取超时分别由 `ITEM_CONNECT_TIMEOUT_MS`、`ITEM_READ_TIMEOUT_MS` 配置。

订单服务仅在商品查询成功且商品在售时保存订单。商品服务返回 `404` 时转换为清晰的 `409` 商品业务错误；其他 Feign 调用异常（例如服务无可用实例时的 `503`）转换为“商品服务暂时不可用”，并返回 HTTP `503`。两种失败路径都在订单写入前结束。实现与调用失败映射见 [OrderService](../../../services/order-service/src/main/java/com/zjgsu/hyh/service/OrderService.java)；Feign 契约见 [ItemClient](../../../services/order-service/src/main/java/com/zjgsu/hyh/client/ItemClient.java)。

### 运行命令与结果

从仓库根目录执行订单服务测试：

```bash
bash monolith/mvnw -B -f services/order-service/pom.xml clean test
```

结果：`BUILD SUCCESS`，`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`。4 项用例分别覆盖成功读取商品并创建订单、商品不存在映射为 `409`、Feign 依赖故障映射为 `503`，以及商品不可购买时不写订单。

需要 Docker、Nacos 和本地 `.env` 才能运行完整服务发现及双实例验证：

```bash
bash scripts/e2e-test.sh
```

当前记录中的真实运行限制：WSL 环境没有 Docker CLI，端到端测试尚未执行；上述单元测试不等同于真实 Nacos/多实例联调。

### 截图

![OpenFeign 客户端按服务名声明调用](screenshots/01-openfeign-client.png)

`ItemClient` 使用 `@FeignClient(name = "item-service", path = "/api/items")`，未出现任何容器 IP。

![远程调用失败处理](screenshots/02-feign-error-handling.png)

`OrderService#fetchItem` 将远程 `404` 转为 `409` 业务冲突，其余 Feign 异常转为 `503` 依赖不可用；`OrderExceptionHandler` 负责输出对应状态码。

![单元测试运行结果](screenshots/03-openfeign-tests.png)

`surefire` 执行 4 项测试全部通过，`BUILD SUCCESS`。

![OpenFeign 依赖与超时配置](screenshots/04-openfeign-config.png)

`spring-cloud-starter-openfeign`、`spring-cloud-starter-loadbalancer` 依赖，以及 `item-service` 的连接与读取超时配置。
