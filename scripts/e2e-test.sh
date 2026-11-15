#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$BASE_DIR"

if ! command -v docker >/dev/null 2>&1 || ! docker info >/dev/null 2>&1; then
  printf 'Docker CLI 或 Docker daemon 不可用，无法运行端到端测试。\n' >&2
  exit 1
fi

if [ -n "$(docker compose ps -q)" ]; then
  printf '已有 Compose 服务正在运行；请先停止后再执行端到端测试。\n' >&2
  exit 1
fi

export ITEM_SERVICE_PORT=0
export ITEM_SERVICE_LOG_LEVEL=DEBUG

docker compose config --quiet
trap 'docker compose down' EXIT
docker compose up -d --build --scale item-service=2

wait_for_health() {
  local url="$1"
  local name="$2"
  for _ in {1..60}; do
    if curl --fail --silent --max-time 2 "$url" | grep -q '"status":"UP"'; then
      printf '%s health check passed\n' "$name"
      return 0
    fi
    sleep 2
  done
  printf '%s health check failed\n' "$name" >&2
  return 1
}

wait_for_database() {
  local service="$1"
  local container
  for _ in {1..60}; do
    container="$(docker compose ps -q "$service" | head -n 1)"
    if [ -n "$container" ] \
      && [ "$(docker inspect --format '{{.State.Health.Status}}' "$container" 2>/dev/null)" = "healthy" ]; then
      printf '%s database health check passed\n' "$service"
      return 0
    fi
    sleep 2
  done
  printf '%s database health check failed\n' "$service" >&2
  return 1
}

wait_for_item_instances() {
  local -a instances=()
  for _ in {1..60}; do
    mapfile -t instances < <(docker compose ps -q item-service)
    if [ "${#instances[@]}" -eq 2 ]; then
      printf '%s\n' "${instances[@]}"
      return 0
    fi
    sleep 2
  done
  printf 'expected 2 running item-service instances, found %s\n' "${#instances[@]}" >&2
  return 1
}

order_count() {
  docker compose exec -T order-db \
    sh -c 'MYSQL_PWD=order_pass mysql -uorder_user secondlight_order -Nse "SELECT COUNT(*) FROM orders"'
}

post_order() {
  local item_id="$1"
  local buyer_id="$2"
  curl --silent --show-error --max-time 5 --write-out $'\n%{http_code}' \
    -H 'Content-Type: application/json' \
    -d "{\"itemId\":\"$item_id\",\"buyerId\":\"$buyer_id\"}" \
    "$ORDER_URL/api/orders"
}

expect_missing_item() {
  local item_id="$1"
  local buyer_id="$2"
  local response
  local status
  local body

  response="$(post_order "$item_id" "$buyer_id")"
  status="${response##*$'\n'}"
  body="${response%$'\n'*}"
  if [ "$status" != "409" ]; then
    printf 'expected missing item to return HTTP 409, received %s\n' "$status" >&2
    return 1
  fi
  printf '%s' "$body" | python3 -c 'import json, sys; assert json.load(sys.stdin)["status"] == 409'
}

expect_dependency_unavailable() {
  local item_id="$1"
  local response
  local status
  local body

  response="$(post_order "$item_id" "e2e-unavailable-${run_id}")"
  status="${response##*$'\n'}"
  body="${response%$'\n'*}"
  if [ "$status" != "503" ]; then
    printf 'expected unavailable item-service to return HTTP 503, received %s\n' "$status" >&2
    return 1
  fi
  printf '%s' "$body" | python3 -c 'import json, sys; assert json.load(sys.stdin)["status"] == 503'
}

wait_for_database item-db
wait_for_database order-db

mapfile -t item_instances < <(wait_for_item_instances)
if [ "${#item_instances[@]}" -ne 2 ]; then
  printf 'expected exactly 2 item-service instances\n' >&2
  exit 1
fi

for instance in "${item_instances[@]}"; do
  port="$(docker inspect --format '{{(index (index .NetworkSettings.Ports "8081/tcp") 0).HostPort}}' "$instance")"
  wait_for_health "http://127.0.0.1:${port}/actuator/health" "item-service $instance"
done

order_port="$(docker compose port order-service 8082 | head -n 1 | sed 's/.*://')"
ORDER_URL="http://127.0.0.1:${order_port}"
wait_for_health "$ORDER_URL/actuator/health" "order-service"

run_id="$(date +%s)-$$"
missing_item_id="e2e-load-balance-${run_id}"
probe_ready=0
for _ in {1..60}; do
  if expect_missing_item "$missing_item_id" "e2e-probe-${run_id}" 2>/dev/null; then
    probe_ready=1
    break
  fi
  sleep 2
done
if [ "$probe_ready" -ne 1 ]; then
  printf 'order-service did not successfully reach item-service\n' >&2
  exit 1
fi

before_count="$(order_count)"
for attempt in {1..30}; do
  expect_missing_item "$missing_item_id" "e2e-${run_id}-${attempt}"
done

for instance in "${item_instances[@]}"; do
  if ! docker logs "$instance" 2>&1 | grep --fixed-strings --quiet "item-service request itemId=${missing_item_id}"; then
    printf 'item-service instance %s did not receive the Feign probe\n' "$instance" >&2
    exit 1
  fi
done

after_count="$(order_count)"
if [ "$before_count" != "$after_count" ]; then
  printf 'failed item checks changed order count: %s -> %s\n' "$before_count" "$after_count" >&2
  exit 1
fi
printf 'Feign load balancing reached both item-service instances; failed checks created no orders\n'

before_unavailable_count="$(order_count)"
docker compose stop item-service >/dev/null
unavailable_ready=0
for _ in {1..60}; do
  if expect_dependency_unavailable "$missing_item_id" 2>/dev/null; then
    unavailable_ready=1
    break
  fi
  sleep 2
done
if [ "$unavailable_ready" -ne 1 ]; then
  printf 'Nacos did not remove the stopped item-service instances in time\n' >&2
  exit 1
fi
after_unavailable_count="$(order_count)"
if [ "$before_unavailable_count" != "$after_unavailable_count" ]; then
  printf 'unavailable item-service changed order count: %s -> %s\n' \
    "$before_unavailable_count" "$after_unavailable_count" >&2
  exit 1
fi
printf 'item-service unavailable returned HTTP 503 without creating an order\n'

docker compose start item-service >/dev/null
mapfile -t item_instances < <(wait_for_item_instances)
for instance in "${item_instances[@]}"; do
  port="$(docker inspect --format '{{(index (index .NetworkSettings.Ports "8081/tcp") 0).HostPort}}' "$instance")"
  wait_for_health "http://127.0.0.1:${port}/actuator/health" "recovered item-service $instance"
done

recovery_ready=0
for _ in {1..60}; do
  response="$(post_order item-1001 "e2e-success-${run_id}")"
  status="${response##*$'\n'}"
  if [ "$status" = "201" ]; then
    recovery_ready=1
    break
  fi
  if [ "$status" != "503" ]; then
    printf 'expected HTTP 503 while waiting for recovery or HTTP 201 after recovery, received %s\n' \
      "$status" >&2
    exit 1
  fi
  sleep 2
done
if [ "$recovery_ready" -ne 1 ]; then
  printf 'order-service did not recover after item-service registration\n' >&2
  exit 1
fi
body="${response%$'\n'*}"
printf '%s' "$body" | python3 -c \
  'import json, sys; order = json.load(sys.stdin); assert order["itemId"] == "item-1001" and order["status"] == "PENDING_PAYMENT" and order["id"]'
printf 'item-service recovery returned HTTP 201 with an order JSON response\n'
