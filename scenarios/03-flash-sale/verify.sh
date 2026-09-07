#!/usr/bin/env bash
# 场景 03 秒杀抢购 curl 冒烟：需后端运行于 localhost:8080（H2 默认配置，种子数据随启动生成）
# 用法：bash scenarios/03-flash-sale/verify.sh
# 可用 FLASH_BASE 覆盖目标地址（默认 8080；例如本机 8080 被其他实例占用时）：
#   FLASH_BASE=http://localhost:8081/api bash scenarios/03-flash-sale/verify.sh
# 注意：
#   1. 请求体保持 ASCII（Windows 控制台编码会把中文载荷损坏成非法 JSON）
#   2. 第 8 步会重置耳机（id=2）的库存与订单，脚本可重复运行（无需重启后端）
set -euo pipefail

BASE="${FLASH_BASE:-http://localhost:8080/api}"
STAMP="$(date +%s)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

json_field() {
  # 从 stdin JSON 提取字段：json_field data.accessToken
  python -c "import sys,json; d=json.load(sys.stdin); [d:=d.get(k) if isinstance(d,dict) else None for k in sys.argv[1].split('.')]; print(d if d is not None else '')" "$1"
}

fail() { echo "冒烟失败：$1" >&2; exit 1; }

step() { echo; echo "=== $1 ==="; }

step "1. 注册冒烟主用户（注册即登录）"
REG=$(curl -sf -X POST "$BASE/auth/register" -H 'Content-Type: application/json' \
  -d "{\"username\":\"smoke${STAMP}\",\"password\":\"pass123456\"}")
ACCESS=$(echo "$REG" | json_field data.accessToken)
if [ -z "$ACCESS" ]; then fail "注册失败：$REG"; fi
echo "注册成功，access=${ACCESS:0:8}…"

step "2. 秒杀商品列表（期望 4 件，状态四档齐备）"
ITEMS=$(curl -sf "$BASE/flash/items" -H "Authorization: Bearer $ACCESS")
COUNT=$(echo "$ITEMS" | python -c "import sys,json; d=json.load(sys.stdin); print(len(d['data']))")
if [ "$COUNT" != "4" ]; then fail "期望 4 件商品，实际 $COUNT：$ITEMS"; fi
STATUS_CHECK=$(echo "$ITEMS" | python -c "
import sys, json
items = {i['id']: i for i in json.load(sys.stdin)['data']}
expect = {1: 'IN_PROGRESS', 2: 'IN_PROGRESS', 3: 'NOT_STARTED', 4: 'ENDED'}
print('ok' if all(items[k]['status'] == v for k, v in expect.items()) else 'bad')")
if [ "$STATUS_CHECK" != "ok" ]; then fail "状态不符：$ITEMS"; fi
STOCK2=$(echo "$ITEMS" | python -c "import sys,json; d=json.load(sys.stdin); print([i['stock'] for i in d['data'] if i['id']==2][0])")
echo "4 件商品状态正确，耳机（id=2）当前库存 $STOCK2"

step "3. 抢购旗舰手机（期望成功）"
ORDER=$(curl -sf -X POST "$BASE/flash/items/1/seckill" -H "Authorization: Bearer $ACCESS")
ORDER_ID=$(echo "$ORDER" | json_field data.orderId)
if [ -z "$ORDER_ID" ]; then fail "抢购未返回订单：$ORDER"; fi
echo "抢购成功，订单号 $ORDER_ID"

step "4. 重复抢购（期望 code=6104）"
CODE=$(curl -s -X POST "$BASE/flash/items/1/seckill" -H "Authorization: Bearer $ACCESS" | json_field code)
if [ "$CODE" != "6104" ]; then fail "期望 6104 重复抢购，实际 $CODE"; fi
echo "限购兜底生效（6104）✓"

step "5. 状态边界：未开始 6101 / 已结束 6102"
CODE3=$(curl -s -X POST "$BASE/flash/items/3/seckill" -H "Authorization: Bearer $ACCESS" | json_field code)
CODE4=$(curl -s -X POST "$BASE/flash/items/4/seckill" -H "Authorization: Bearer $ACCESS" | json_field code)
if [ "$CODE3" != "6101" ] || [ "$CODE4" != "6102" ]; then
  fail "期望 6101/6102，实际 $CODE3/$CODE4"
fi
echo "未开始 6101 ✓  已结束 6102 ✓"

step "6. 并发演示：12 个用户并发抢耳机 5 件库存（期望恰好 5 成功 7 售罄）"
TOKENS=()
for i in $(seq 1 12); do
  T=$(curl -sf -X POST "$BASE/auth/register" -H 'Content-Type: application/json' \
    -d "{\"username\":\"race${STAMP}_$i\",\"password\":\"pass123456\"}" | json_field data.accessToken)
  if [ -z "$T" ]; then fail "注册并发用户 $i 失败"; fi
  TOKENS+=("$T")
done
for i in $(seq 1 12); do
  (curl -s -X POST "$BASE/flash/items/2/seckill" -H "Authorization: Bearer ${TOKENS[$((i-1))]}" \
    | json_field code > "$TMP/race-$i") &
done
wait
WINS=$(grep -l '^200$' "$TMP"/race-* | wc -l)
SOLD=$(grep -l '^6103$' "$TMP"/race-* | wc -l)
echo "并发结果：成功 $WINS 单 / 售罄拒绝 $SOLD 单"
if [ "$WINS" != "5" ] || [ "$SOLD" != "7" ]; then
  fail "期望恰好 5 成功 7 售罄（零超卖），实际 $WINS/$SOLD"
fi
FINAL=$(curl -sf "$BASE/flash/items" -H "Authorization: Bearer $ACCESS" \
  | python -c "import sys,json; d=json.load(sys.stdin); print([i['stock'] for i in d['data'] if i['id']==2][0])")
if [ "$FINAL" != "0" ]; then fail "售罄后库存应为 0，实际 $FINAL"; fi
echo "零超卖对账通过：5 单 + 0 库存 == 初始 5 库存 ✓"

step "7. 我的订单（主用户应含旗舰手机订单）"
MINE=$(curl -sf "$BASE/flash/orders/mine" -H "Authorization: Bearer $ACCESS")
N=$(echo "$MINE" | python -c "import sys,json; d=json.load(sys.stdin); print(sum(1 for o in d['data'] if o['itemId']==1))")
if [ "$N" != "1" ]; then fail "期望主用户有 1 笔旗舰手机订单，实际 $N：$MINE"; fi
echo "订单查询正确 ✓"

step "8. 重置演示库存（耳机回到 5 件，脚本可重复运行）"
CODE=$(curl -s -X POST "$BASE/flash/demo/reset/2" -H "Authorization: Bearer $ACCESS" | json_field code)
if [ "$CODE" != "200" ]; then fail "期望重置成功 200，实际 $CODE"; fi
STOCK2=$(curl -sf "$BASE/flash/items" -H "Authorization: Bearer $ACCESS" \
  | python -c "import sys,json; d=json.load(sys.stdin); print([i['stock'] for i in d['data'] if i['id']==2][0])")
if [ "$STOCK2" != "5" ]; then fail "重置后耳机库存应为 5，实际 $STOCK2"; fi
echo "重置成功，耳机库存回满 $STOCK2 ✓"

step "9. 已结束活动不可重置（期望 code=417）"
CODE=$(curl -s -X POST "$BASE/flash/demo/reset/4" -H "Authorization: Bearer $ACCESS" | json_field code)
if [ "$CODE" != "417" ]; then fail "期望 417，实际 $CODE"; fi
echo "状态守卫生效（417）✓"

echo
echo "全部冒烟步骤通过 ✅"
