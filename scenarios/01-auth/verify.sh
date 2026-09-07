#!/usr/bin/env bash
# 场景 01 认证链路 curl 冒烟：需后端运行于 localhost:8080（H2 默认配置）
# 用法：bash scenarios/01-auth/verify.sh
# 注意：请求体保持 ASCII（Windows 控制台编码会把中文载荷损坏成非法 JSON）
set -euo pipefail

BASE=http://localhost:8080/api
USERNAME="u$(date +%s)"

json_field() {
  # 从 stdin JSON 提取字段：json_field data.accessToken
  python -c "import sys,json; d=json.load(sys.stdin); [d:=d.get(k) if isinstance(d,dict) else None for k in sys.argv[1].split('.')]; print(d if d is not None else '')" "$1"
}

fail() { echo "冒烟失败：$1" >&2; exit 1; }

step() { echo; echo "=== $1 ==="; }

step "1. 注册（$USERNAME）"
REG=$(curl -sf -X POST "$BASE/auth/register" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USERNAME\",\"password\":\"pass123456\",\"nickname\":\"smoke\"}")
ACCESS=$(echo "$REG" | json_field data.accessToken)
REFRESH=$(echo "$REG" | json_field data.refreshToken)
if [ -z "$ACCESS" ] || [ -z "$REFRESH" ]; then fail "注册未返回 token 对：$REG"; fi
echo "注册成功，access=${ACCESS:0:8}…"

step "2. 受保护接口 /user/me（携带 access）"
ME=$(curl -sf "$BASE/user/me" -H "Authorization: Bearer $ACCESS" | json_field data.username)
if [ "$ME" != "$USERNAME" ]; then fail "me 返回用户名不符：$ME"; fi
echo "me 返回用户名正确"

step "3. 无 token 访问 /user/me（期望 code=401）"
CODE=$(curl -s "$BASE/user/me" | json_field code)
if [ "$CODE" != "401" ]; then fail "期望 401，实际 $CODE"; fi
echo "未携带 token 返回 401 ✓"

step "4. 伪造 token（期望 code=401）"
CODE=$(curl -s "$BASE/user/me" -H "Authorization: Bearer abc.def.ghi" | json_field code)
if [ "$CODE" != "401" ]; then fail "期望 401，实际 $CODE"; fi
echo "伪造 token 返回 401 ✓"

step "5. refresh 旋转换新"
NEW_ACCESS=$(curl -sf -X POST "$BASE/auth/refresh" -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$REFRESH\"}" | json_field data.accessToken)
if [ -z "$NEW_ACCESS" ]; then fail "refresh 未返回新 access"; fi
if [ "$NEW_ACCESS" = "$ACCESS" ]; then fail "refresh 后 access 未变化"; fi
echo "新 access=${NEW_ACCESS:0:8}…（与旧值不同）✓"

step "6. demo 种子账号登录"
LOGIN=$(curl -sf -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"demo123456"}')
LOGIN_ACCESS=$(echo "$LOGIN" | json_field data.accessToken)
if [ -z "$LOGIN_ACCESS" ]; then fail "demo 账号登录失败：$LOGIN"; fi
echo "demo 账号登录成功 ✓"

step "7. 密码错误（期望 code=401 且文案统一）"
WRONG=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"wrong-password"}')
WRONG_CODE=$(echo "$WRONG" | json_field code)
WRONG_MSG=$(echo "$WRONG" | json_field msg)
if [ "$WRONG_CODE" != "401" ] || [ "$WRONG_MSG" != "用户名或密码错误" ]; then
  fail "期望 401/统一文案，实际 $WRONG_CODE/$WRONG_MSG"
fi
echo "错误文案统一 ✓"

echo
echo "全部冒烟步骤通过 ✅"
