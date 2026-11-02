# 第七周：Nacos 服务注册与发现

## 作业范围

本周在第六周已有的 `item-service`、`order-service` 和 `user-service` 中接入
Nacos Discovery，并在根目录 `compose.yaml` 中增加 Nacos 3.2.4 编排。
保留原有服务名、数据归属、接口和业务规则；本阶段只完成服务注册配置，
跨服务调用改为服务发现的验证留待后续小节。

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
| Nacos 控制台服务列表、分组和三个实例 | 待补充 |
| 三个业务服务健康检查 | 待补充 |

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
