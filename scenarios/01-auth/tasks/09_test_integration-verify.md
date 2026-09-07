---
id: auth-jwt_09_test_integration_verify
name: "前后端联调验证（curl 冒烟 + 手动验收）"
type: test
subtype: null
status: completed
blocked_reason: null
depends: [auth-jwt_05_test_auth_flow, auth-jwt_08_ui_auth_pages]
profiles: [backend, frontend, testing]
files:
  - scenarios/01-auth/verify.sh
---

# 前后端联调验证（curl 冒烟 + 手动验收）

## 任务范围

| 项目 | 内容 |
|------|------|
| 类型 | `test` |
| 已启用 profile | backend / frontend / testing |
| 架构边界 | 联调属独立验证步骤，不并入前后端任一侧；产物为可重复执行的冒烟脚本 + 手动验收记录 |
| 结构分析 | 环境事实：后端 8080（context-path /api）、前端 5173（代理 /api → 8080，无 rewrite） |
| 完成条件 | curl 冒烟脚本全部步骤通过；手动验收清单逐项打勾并记录到任务文件 |

## 需求与验收

- 用户目标：在真实运行环境确认冻结契约端到端成立。
- 包含：curl 冒烟脚本（注册→me→无 token 401→伪造 401→refresh→demo 登录）+ 浏览器手动验收清单。
- 不包含：自动化 e2e（项目无该设施，如实记录）。
- 验收：脚本退出码 0；手动清单无未通过项。

## 设计与依赖

| 类别 | 事实 / 决策 |
|------|-------------|
| 输入与输出 | 脚本消费契约接口；JSON 字段提取用 python（仓库已验证可用），不依赖 jq |
| 数据/Schema | 注册用时间戳用户名，不污染种子数据 |
| 集成 | 需要后端运行于 localhost:8080（H2 默认配置） |
| 安全与质量 | 脚本只打印 token 前后各 8 字符与响应摘要，不完整落日志 |
| 依赖 | 05（后端链路绿）、08（前端页面就绪） |

## 实现步骤

1. 落盘 `verify.sh`；
2. 启动后端（`java -jar` 或 IDE），执行 `bash scenarios/01-auth/verify.sh`；
3. 启动前端 `npm run dev`，按清单手动验收；
4. 结果（通过/失败截图或描述）回写本任务「风险与阻塞」下的验证记录。

## 涉及文件

| 文件 | 模块/区域 | 操作 | 说明 |
|------|-----------|------|------|
| `scenarios/01-auth/verify.sh` | 场景目录 | 新增 | curl 冒烟脚本 |

## 完整代码（供手动敲写）

### scenarios/01-auth/verify.sh（新增）

```bash
#!/usr/bin/env bash
# 场景 01 认证链路 curl 冒烟：需后端运行于 localhost:8080（H2 默认配置）
# 用法：bash scenarios/01-auth/verify.sh
set -euo pipefail

BASE=http://localhost:8080/api
USERNAME="u$(date +%s)"

json_field() {
  # 从 stdin JSON 提取字段：json_field data.accessToken
  python -c "import sys,json; d=json.load(sys.stdin); [d:=d.get(k) if isinstance(d,dict) else None for k in sys.argv[1].split('.')]; print(d if d is not None else '')" "$1"
}

step() { echo; echo "=== $1 ==="; }

step "1. 注册（$USERNAME）"
REG=$(curl -sf -X POST "$BASE/auth/register" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USERNAME\",\"password\":\"pass123456\",\"nickname\":\"冒烟用户\"}")
ACCESS=$(echo "$REG" | json_field data.accessToken)
REFRESH=$(echo "$REG" | json_field data.refreshToken)
test -n "$ACCESS" && test -n "$REFRESH"
echo "注册成功，access=${ACCESS:0:8}…"

step "2. 受保护接口 /user/me（携带 access）"
curl -sf "$BASE/user/me" -H "Authorization: Bearer $ACCESS" | json_field data.username | grep -qx "$USERNAME"
echo "me 返回用户名正确"

step "3. 无 token 访问 /user/me（期望 code=401）"
CODE=$(curl -s "$BASE/user/me" | json_field code)
test "$CODE" = "401"
echo "未携带 token 返回 401 ✓"

step "4. 伪造 token（期望 code=401）"
CODE=$(curl -s "$BASE/user/me" -H "Authorization: Bearer abc.def.ghi" | json_field code)
test "$CODE" = "401"
echo "伪造 token 返回 401 ✓"

step "5. refresh 旋转换新"
NEW_ACCESS=$(curl -sf -X POST "$BASE/auth/refresh" -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$REFRESH\"}" | json_field data.accessToken)
test -n "$NEW_ACCESS" && test "$NEW_ACCESS" != "$ACCESS"
echo "新 access=${NEW_ACCESS:0:8}…（与旧值不同）✓"

step "6. demo 种子账号登录"
LOGIN=$(curl -sf -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"demo123456"}')
test -n "$(echo "$LOGIN" | json_field data.accessToken)"
echo "demo 账号登录成功 ✓"

step "7. 密码错误（期望 code=401 且文案统一）"
WRONG=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"wrong-password"}')
test "$(echo "$WRONG" | json_field code)" = "401"
test "$(echo "$WRONG" | json_field msg)" = "用户名或密码错误"
echo "错误文案统一 ✓"

echo
echo "全部冒烟步骤通过 ✅"
```

## 验证（手动验收清单）

前置：后端 `mvn clean package && java -jar demo/demo-starter/target/demo-starter-0.0.1-SNAPSHOT.jar`；前端 `npm run dev`。

| # | 步骤 | 预期 |
|---|------|------|
| 1 | 打开 http://localhost:5173 未登录 | 重定向 `/login?redirect=/` |
| 2 | 注册新用户 | 成功后直接进入首页，用户卡显示昵称 |
| 3 | 刷新页面（F5） | 登录态保持（token 从 localStorage 恢复，用户卡重新拉取） |
| 4 | 登出 | 回到登录页；再次访问 `/` 被拦 |
| 5 | demo/demo123456 登录 | 进入首页 |
| 6 | 密码错误 | 红字「用户名或密码错误」，不跳转 |
| 7 | 无感续期观察 | 将 access-expire-seconds 临时调为 30 重启后端，登录后等待过期再操作页面，请求自动成功（无感续期） |
| 8 | 全过期 | 等 refresh 也过期（临时调 60），操作后跳登录页 |

## 风险与阻塞

- 风险：无感续期观察项依赖临时改配置，验证后必须改回 1800/604800 并同步测试 yml。
- 阻塞：无。
- 验证记录（2026-09-05 执行完毕）：
  - curl 冒烟：7/7 步通过（最终配置复跑再次通过）；
  - 浏览器手动验收 8/8：①未登录访问 `/` 重定向 `/login?redirect=/` ②注册即登录进入首页（昵称正常）③F5 登录态保持 ④登出回登录页 ⑤demo 登录进入首页 ⑥密码错误红字不跳转 ⑦无感续期（临时 TTL 30s/90s，t0+40s 刷新页面触发 /user/me 401 → 静默刷新重放，用户卡正常）⑧全过期（最后续期+100s 刷新 → 清空跳 `/login?redirect=/`）；
  - 联调发现并修复的缺陷：data.sql 中文种子在 Windows 平台编码下乱码（`spring.sql.init.encoding: UTF-8` 修复，主/测试 yml 同步）；
  - 验证后 TTL 已恢复 1800/604800 并重新打包复跑冒烟通过。

修订：1 - 冒烟脚本两处修正：请求载荷去中文（Windows 控制台编码损坏 JSON）+ `test A && test B` 改显式守卫（`set -e` 对 && 列表非末位失败有豁免陷阱）。

修订：2 - 无感续期验证载体从「健康检查按钮」改为「页面刷新触发 /user/me」：/health 属白名单不经过过滤器，无法触发 401 分支。
