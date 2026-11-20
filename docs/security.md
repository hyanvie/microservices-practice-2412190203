# 安全设计

## 1. 认证流程

1. 客户端向网关 `POST /api/users/login` 提交用户 ID 和密码，该路径允许匿名访问。
2. `user-service` 从 `user_credentials` 读取 PBKDF2-HMAC-SHA256 密码哈希并校验，明文密码不落库。
3. 校验通过后，`user-service` 使用 HS256 签发 JWT，声明包含 `sub`（用户 ID）、`iss`、`iat`、`exp` 和 `roles`。
4. 客户端后续请求携带 `Authorization: Bearer <token>`。
5. 网关使用与签发方相同的 `JWT_SECRET` 验签并检查有效期，失败返回 401；再按 `roles` 声明映射为 `ROLE_BUYER`、`ROLE_SELLER`、`ROLE_ADMIN` 判断接口权限，越权返回 403。
6. 网关删除客户端自带的 `X-User-Id`、`X-User-Roles`，仅在验签通过后写入可信身份头再转发。
7. `user-service` 仍按 Resource Server 方式独立验签，不依赖网关注入的身份头。

JWT 密钥只从环境变量 `JWT_SECRET` 注入，要求至少 32 个 UTF-8 字节；`.env` 和真实密钥不进入版本库。

## 2. 角色与权限

| 角色 | 可访问资源 |
|---|---|
| 匿名 | `POST /api/users/login`、`GET /api/users/{userId}/exists`、`GET /api/items/**`、`GET /actuator/health` |
| BUYER | 买家资料 `/api/users/buyer/**`、商品锁定 `POST /api/items/{itemId}/reserve`、创建订单 `POST /api/orders` |
| SELLER | 卖家资料 `/api/users/seller/**` |
| ADMIN | 管理概览 `/api/users/admin/**` |
| 已登录用户 | 其余网关路由，包括订单查询 `GET /api/orders/**` |

课程演示账号：

| 用户 ID | 密码 | 角色 |
|---|---|---|
| buyer-1001 | BuyerDemo!2026 | BUYER |
| seller-1001 | SellerDemo!2026 | SELLER |
| seller-1002 | SellerDemo!2026 | SELLER |
| admin-1001 | AdminDemo!2026 | ADMIN |

演示凭据仅用于本地课程环境。

## 3. 网关路由表

| 路由 ID | 路径 | 下游服务 | 鉴权规则 |
|---|---|---|---|
| `user-service` | `/api/users/**` | `user-service:8083` | 登录、存在性查询匿名；管理接口需 ADMIN；买家/卖家资料按角色；其余需登录 |
| `item-service` | `/api/items/**` | `item-service:8081` | `GET` 匿名；`POST .../reserve` 需 BUYER |
| `order-service` | `/api/orders/**` | `order-service:8082` | 创建订单需 BUYER；其余需登录 |

网关按服务名通过 Nacos 发现下游实例，不硬编码容器 IP。健康检查 `/actuator/health` 允许匿名访问，供 Compose 和排查使用。

## 4. 伪造身份头与绕过网关

- 客户端可以伪造任意请求头，因此 `IdentityHeaderFilter` 在转发前无条件删除 `X-User-Id` 和 `X-User-Roles`，只在 JWT 验签通过后按 `sub` 和 `roles` 重新写入。伪造这些头不能改变网关的鉴权结果。
- 下游服务不把身份头作为授权依据：`user-service` 自己验签 JWT；`item-service`、`order-service` 不使用身份头做业务判断。可信身份头只用于后续需要用户上下文的内部调用。
- Compose 中业务服务的宿主机端口仅绑定 `127.0.0.1`，属于本机排查用途；生产部署应删除这些端口映射，只暴露网关端口，业务服务留在 Compose 内部网络。外部客户端无法通过宿主机网络直达业务服务。
- 当前 `order-service`、`item-service` 尚未独立校验 JWT。若必须保留宿主机调试端口，应只在受信任主机使用；上线前需补充服务间鉴权或网络策略，不能把“端口未公开”当作唯一防线。
- 网关是统一入口，路由规则、角色判断和身份头清洗都在网关完成；下游服务不能假设请求一定经过网关，因此关键服务仍保留自身校验。

## 5. 验证方法

在仓库根目录启动依赖、业务服务与网关后执行：

```bash
# 未登录访问受保护接口，预期 401
curl -i http://localhost:8080/api/orders

# 登录获取 Token，再访问受保护接口，预期 200
BUYER_TOKEN=$(curl -fsS -H 'Content-Type: application/json' \
  -d '{"userId":"buyer-1001","password":"BuyerDemo!2026"}' \
  http://localhost:8080/api/users/login | jq -r .accessToken)
curl -i -H "Authorization: Bearer $BUYER_TOKEN" \
  http://localhost:8080/api/users/buyer/profile

# 买家访问卖家接口，预期 403
curl -i -H "Authorization: Bearer $BUYER_TOKEN" \
  http://localhost:8080/api/users/seller/profile

# 伪造身份头但不带 Token，预期 401
curl -i -H 'X-User-Id: forged-admin' -H 'X-User-Roles: SELLER' \
  http://localhost:8080/api/users/seller/profile

# 买家 Token 叠加伪造 SELLER 头，预期仍为 403
curl -i -H "Authorization: Bearer $BUYER_TOKEN" \
  -H 'X-User-Id: forged-admin' -H 'X-User-Roles: SELLER' \
  http://localhost:8080/api/users/seller/profile

# 低权限 Token 访问管理接口，预期 403
curl -i -H "Authorization: Bearer $BUYER_TOKEN" \
  http://localhost:8080/api/users/admin/overview
```

自动化检查脚本 `scripts/auth-test.sh` 覆盖未登录、登录、授权和越权四类场景，任一失败以非零状态退出：

```bash
bash scripts/auth-test.sh
```

实际执行结果为 5 项检查全部通过；管理员 `admin-1001` 访问 `/api/users/admin/overview` 返回 200，买家 Token 访问同一接口返回 403。

网关返回的 401/403 为 JSON，包含 `status`、`error`、`message` 和 `path`。运行截图见
[`docs/homework/week-09/screenshots/`](homework/week-09/screenshots/)。
