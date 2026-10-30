# Week 05：Docker 容器化与编排

## 目标

将本周完成的 `monolith/` 单体 Spring Boot 应用容器化，并通过 Docker Compose 一键启动应用与 MySQL。目标包括：

- 使用多阶段构建的 `Dockerfile`，构建阶段使用 Maven 镜像，运行阶段使用 JRE 镜像。
- 在仓库根目录编排 `compose.yaml`：一个 MySQL 容器 + 一个应用容器。
- 使用 `.env.example` 作为环境变量模板，保留数据库和应用端口配置。
- 验证应用可通过容器访问并在容器重建后保留数据。

## 1. Dockerfile 说明

多阶段构建的核心思想是把“构建依赖”和“运行依赖”分离：

- 构建阶段使用 `maven:3.9.11-eclipse-temurin-25`，执行 `mvn clean package` 生成可执行 JAR。
- 运行阶段使用 `eclipse-temurin:25-jre`，只保留最小运行时环境，减小镜像体积并提高可移植性。
- 这样做的好处是：不需要本机安装 Maven 或 Java，也能在 CI/CD 或其它环境里稳定构建镜像。

在仓库根目录执行：

```bash
docker build -t secondlight:week05 ./monolith
```

构建成功后可查看镜像：

```bash
docker images | grep secondlight
docker image inspect secondlight:week05
```

## 2. Docker Compose 编排

项目根目录中的 `compose.yaml` 负责一键启动：

```bash
cp .env.example .env

docker compose up -d --build
```

启动后，可查看两个容器：

```bash
docker compose ps
```

Compose 包含 `app` 和 `mysql` 两个服务，其中 MySQL 使用
`mysql:8.4.11`，应用的构建上下文为 `./monolith`。两个服务使用 Compose
默认共享网络，应用通过服务名 `mysql` 访问数据库，而不是使用宿主机地址。
MySQL 数据保存于命名卷 `mysql-data`，应用使用 `depends_on` 的
`service_healthy` 条件等待 MySQL 健康检查通过后再启动。

应用容器会通过环境变量连接 MySQL：

- `DB_URL=jdbc:mysql://mysql:3306/secondlight?...`
- `DB_USERNAME=secondlight`
- `DB_PASSWORD=secondlightpass`

容器连接参数与业务数据保持一致。MySQL 数据挂载在 `mysql-data` 卷上，容器重建后数据不会丢失。

本小步建议保存以下启动证明截图：

- `03-compose-ps-success.png`：执行 `docker compose ps`，清楚显示
  `secondlight-app` 和 `secondlight-mysql` 均为运行状态，MySQL 显示
  `healthy`。
- `04-compose-logs-success.png`：执行
  `docker compose logs --tail 100 app`，清楚显示应用连接 MySQL、
  `Tomcat started` 和 `Started MonolithApplication`。

## 3. 业务 API 验证

应用启动后，使用以下接口验证容器内应用可正常访问数据库：

```bash
curl -i http://localhost:8080/api/items
curl -i http://localhost:8080/api/items/item-1001
curl -i -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"itemId":"item-1001","buyerId":"buyer-1001"}'
```

其中：

- `/api/items` 返回商品列表，应用启动时会初始化演示商品。
- `/api/items/item-1001` 返回商品详情。
- `POST /api/orders` 可创建订单，创建后数据库中会新增订单记录，商品状态由 `ON_SALE` 转为 `SOLD`。

## 4. 数据持久化验证

由 MySQL 挂载卷 `mysql-data` 负责持久化业务数据。验证步骤如下：

```bash
docker exec -it secondlight-mysql mysql -u root -prootpass -e "USE secondlight; SHOW TABLES; SELECT id, title, status FROM items;"
```

如果服务重启或容器重建后仍能看到 `items`、`orders` 表及已创建的订单记录，说明数据持久化配置正确。

## 5. 运行结果标记

本周容器化工作完成后，建议保留以下文件与证据：

- `monolith/Dockerfile`
- `monolith/.dockerignore`
- `compose.yaml`
- `.env.example`
- `docs/homework/week-05/index.md`
- `docs/homework/week-05/screenshots/` 中的运行截图

## 6. 说明

本说明仅作为周作业记录模板，实际提交时请按个人项目替换镜像名、端口和截图名称，并在 GitHub README 或作业页中补充构建命令、运行命令和验证结果。

## 7. 常见启动问题

如果 `docker build -t secondlight:week05 ./monolith` 成功，但
`docker compose up --build -d` 报告 `3306` 端口已被占用，这是宿主机端口冲突，
不是镜像构建失败。应用容器仍通过 Compose 网络使用 `mysql:3306`，只需要更换
MySQL 暴露到宿主机的端口：

```powershell
$env:MYSQL_PORT="3307"
docker compose up --build -d
```

也可以在根目录 `.env` 中将 `MYSQL_PORT` 改为未占用的端口。此时应用 API
仍然从 `http://localhost:8080` 访问，数据库容器内部端口无需修改。
