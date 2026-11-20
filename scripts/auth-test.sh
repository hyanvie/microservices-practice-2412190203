#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
BUYER_ID="${BUYER_ID:-buyer-1001}"
BUYER_PASSWORD="${BUYER_PASSWORD:-BuyerDemo!2026}"

if ! command -v curl >/dev/null 2>&1 || ! command -v jq >/dev/null 2>&1; then
  printf '需要 curl 和 jq，请先安装后再运行。\n' >&2
  exit 1
fi

http_call() {
  local output
  output=$(curl -sS -w $'\n%{http_code}' "$@")
  HTTP_BODY="${output%$'\n'*}"
  HTTP_STATUS="${output##*$'\n'}"
}

pass=0
fail=0

check_status() {
  local name="$1"
  local expected="$2"
  if [ "$HTTP_STATUS" = "$expected" ]; then
    printf '[PASS] %s：期望 %s，实际 %s\n' "$name" "$expected" "$HTTP_STATUS"
    pass=$((pass + 1))
  else
    printf '[FAIL] %s：期望 %s，实际 %s\n' "$name" "$expected" "$HTTP_STATUS" >&2
    fail=$((fail + 1))
  fi
}

http_call "$BASE_URL/api/users/buyer/profile"
check_status "未登录访问受保护接口" 401

http_call -H 'Content-Type: application/json' \
  -d "{\"userId\":\"$BUYER_ID\",\"password\":\"$BUYER_PASSWORD\"}" \
  "$BASE_URL/api/users/login"
check_status "登录获取 Token" 200

token=$(printf '%s' "$HTTP_BODY" | jq -r '.accessToken // empty')
if [ -z "$token" ]; then
  printf '[FAIL] 登录响应缺少 accessToken\n' >&2
  fail=$((fail + 1))
else
  http_call -H "Authorization: Bearer $token" "$BASE_URL/api/users/buyer/profile"
  check_status "携带 Token 访问受保护接口" 200
  if printf '%s' "$HTTP_BODY" | jq -e '.userId == "buyer-1001"' >/dev/null 2>&1; then
    printf '[PASS] Token 身份与登录用户一致：userId=buyer-1001\n'
    pass=$((pass + 1))
  else
    printf '[FAIL] Token 身份与登录用户不一致\n' >&2
    fail=$((fail + 1))
  fi
fi

http_call -H "Authorization: Bearer $token" "$BASE_URL/api/users/admin/overview"
check_status "低权限 Token 访问管理接口" 403

if [ "$fail" -ne 0 ]; then
  printf '检查失败 %d 项，通过 %d 项。\n' "$fail" "$pass" >&2
  exit 1
fi
printf '全部 %d 项检查通过。\n' "$pass"
