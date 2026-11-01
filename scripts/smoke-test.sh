#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$BASE_DIR"

wait_for_health() {
  local url="$1"
  local name="$2"
  for _ in {1..30}; do
    if curl --fail --silent "$url" | grep -q '"status":"UP"'; then
      printf '%s health check passed\n' "$name"
      return 0
    fi
    sleep 2
  done
  printf '%s health check failed\n' "$name" >&2
  return 1
}

wait_for_database() {
  local container="$1"
  for _ in {1..30}; do
    if [ "$(docker inspect --format '{{.State.Health.Status}}' "$container" 2>/dev/null)" = "healthy" ]; then
      printf '%s database health check passed\n' "$container"
      return 0
    fi
    sleep 2
  done
  printf '%s database health check failed\n' "$container" >&2
  return 1
}

expect_http_status() {
  local method="$1"
  local url="$2"
  local expected="$3"
  local body="${4:-}"
  local actual

  if [ -n "$body" ]; then
    actual="$(curl --silent --show-error --output /dev/null --write-out '%{http_code}' \
      -X "$method" -H 'Content-Type: application/json' -d "$body" "$url")"
  else
    actual="$(curl --silent --show-error --output /dev/null --write-out '%{http_code}' \
      -X "$method" "$url")"
  fi

  if [ "$actual" != "$expected" ]; then
    printf '%s expected HTTP %s but received %s\n' "$url" "$expected" "$actual" >&2
    return 1
  fi
  printf '%s HTTP %s as expected\n' "$url" "$actual"
}

docker compose up -d --build
trap 'docker compose down' EXIT

wait_for_database "secondlight-user-db"
wait_for_database "secondlight-item-db"
wait_for_database "secondlight-order-db"
wait_for_health "http://localhost:${USER_SERVICE_PORT:-8083}/actuator/health" "user-service"
wait_for_health "http://localhost:${ITEM_SERVICE_PORT:-8081}/actuator/health" "item-service"
wait_for_health "http://localhost:${ORDER_SERVICE_PORT:-8082}/actuator/health" "order-service"

curl --fail --silent "http://localhost:${ITEM_SERVICE_PORT:-8081}/api/items/item-1001" >/dev/null
curl --fail --silent -X POST \
  -H 'Content-Type: application/json' \
  -d '{"itemId":"item-1001","buyerId":"buyer-1001"}' \
  "http://localhost:${ORDER_SERVICE_PORT:-8082}/api/orders" >/dev/null
printf 'cross-service order smoke test passed\n'

expect_http_status POST \
  "http://localhost:${ORDER_SERVICE_PORT:-8082}/api/orders" \
  "409" \
  '{"itemId":"missing-item","buyerId":"buyer-1001"}'

docker compose stop item-service >/dev/null
expect_http_status POST \
  "http://localhost:${ORDER_SERVICE_PORT:-8082}/api/orders" \
  "503" \
  '{"itemId":"item-1002","buyerId":"buyer-1001"}'
