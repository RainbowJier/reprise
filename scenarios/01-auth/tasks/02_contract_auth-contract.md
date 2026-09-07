---
id: auth-jwt_02_contract_auth_contract
name: "认证契约层（上下文/DTO/服务端口接口）"
type: contract
subtype: null
status: completed
blocked_reason: null
depends: []
profiles: [backend, api]
files:
  - backend/demo/demo-client/pom.xml
  - backend/demo/demo-shared/src/main/java/com/fullstack/demo/shared/auth/LoginUser.java
  - backend/demo/demo-shared/src/main/java/com/fullstack/demo/shared/auth/UserContextHolder.java
  - backend/demo/demo-shared/src/main/java/com/fullstack/demo/shared/auth/AuthConstants.java
  - backend/demo/demo-client/src/main/java/com/fullstack/demo/client/dto/auth/RegisterReq.java
  - backend/demo/demo-client/src/main/java/com/fullstack/demo/client/dto/auth/LoginReq.java
  - backend/demo/demo-client/src/main/java/com/fullstack/demo/client/dto/auth/RefreshReq.java
  - backend/demo/demo-client/src/main/java/com/fullstack/demo/client/dto/auth/TokenResp.java
  - backend/demo/demo-client/src/main/java/com/fullstack/demo/client/dto/user/UserInfoResp.java
  - backend/demo/demo-client/src/main/java/com/fullstack/demo/client/service/TokenProvider.java
  - backend/demo/demo-client/src/main/java/com/fullstack/demo/client/service/AuthService.java
  - backend/demo/demo-client/src/main/java/com/fullstack/demo/client/service/UserService.java
---

# 认证契约层（上下文/DTO/服务端口接口）

## 任务范围

| 项目 | 内容 |
|------|------|
| 类型 | `contract` |
| 已启用 profile | backend / api |
| 架构边界 | demo-shared（认证上下文与常量）；demo-client（DTO 与服务/端口接口，实现位于 application 与 infrastructure） |
| 结构分析 | 只读降级分析：接口契约即设计文档「六、接口设计」（已冻结）；demo-client 依赖 demo-shared 已核实 |
| 完成条件 | `mvn test` 通过（新增类型仅编译，无行为变化） |

## 需求与验收

- 用户目标：冻结代码级契约——请求/响应 DTO、服务接口、TokenProvider 端口、用户上下文，供前后端两侧并行开发。
- 包含：3 个 shared 类、5 个 DTO、3 个接口。
- 不包含：任何实现（application/infrastructure 在任务 03）。
- 验收：编译通过；DTO 校验注解与设计文档一致（用户名 4-32 字母数字下划线、密码 6-64）。

## 设计与依赖

| 类别 | 事实 / 决策 |
|------|-------------|
| 输入与输出 | 契约对应 REST 接口：POST /auth/register、/auth/login、/auth/refresh、GET /user/me |
| 数据/Schema | 不适用 |
| 集成 | TokenProvider 为依赖倒置端口：接口在 client，jjwt 实现在 infrastructure（任务 03） |
| 安全与质量 | TokenResp 不含敏感信息；LoginUser 仅 userId + username |
| 依赖 | 无前置任务（与 01 并行） |

## 实现步骤

1. demo-shared 新建 `auth` 包：`LoginUser`、`UserContextHolder`、`AuthConstants`；
2. demo-client 新建 `dto.auth` / `dto.user` 包的 5 个 DTO（jakarta validation，shared 已含 validation starter）；
3. demo-client `service` 包新建 `AuthService`、`UserService`、`TokenProvider`；
4. `mvn test` 验证编译。

## 涉及文件

| 文件 | 模块/区域 | 操作 | 说明 |
|------|-----------|------|------|
| `backend/demo/demo-shared/src/main/java/com/fullstack/demo/shared/auth/*.java` | demo-shared | 新增 | 上下文载体/ThreadLocal/常量 |
| `backend/demo/demo-client/src/main/java/com/fullstack/demo/client/dto/auth/*.java` | demo-client | 新增 | 请求/响应 DTO |
| `backend/demo/demo-client/src/main/java/com/fullstack/demo/client/dto/user/UserInfoResp.java` | demo-client | 新增 | 用户信息响应 |
| `backend/demo/demo-client/src/main/java/com/fullstack/demo/client/service/*.java` | demo-client | 新增 | 服务与端口接口 |

## 完整代码（供手动敲写）

### backend/demo/demo-client/pom.xml（修改）

```xml
<!-- dependencies 段内追加（common-mybatis-plus 之后）；版本由 Spring Boot BOM 管理 -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
</dependency>
```

修改点说明：DTO 使用 lombok 注解，demo-client 原先未声明 lombok（探测时遗漏，执行期 `mvn test` 编译失败后补齐）。落点：`<dependencies>` 内。

修订：1 - 执行期发现 demo-client 缺 lombok 依赖导致编译失败，补充 provided 依赖后通过。

### backend/demo/demo-shared/src/main/java/com/fullstack/demo/shared/auth/LoginUser.java（新增）

```java
package com.fullstack.demo.shared.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录用户上下文载体（来自 access token 解析结果）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID（token sub）
     */
    private Long userId;

    /**
     * 登录名
     */
    private String username;
}
```

### backend/demo/demo-shared/src/main/java/com/fullstack/demo/shared/auth/UserContextHolder.java（新增）

```java
package com.fullstack.demo.shared.auth;

/**
 * 当前请求用户上下文（ThreadLocal）。
 * <p>
 * 由 JwtAuthFilter 写入并在 finally 中清理；application 层服务只读。
 */
public final class UserContextHolder {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
```

### backend/demo/demo-shared/src/main/java/com/fullstack/demo/shared/auth/AuthConstants.java（新增）

```java
package com.fullstack.demo.shared.auth;

/**
 * 认证相关常量
 */
public final class AuthConstants {

    /**
     * token 请求头
     */
    public static final String TOKEN_HEADER = "Authorization";

    /**
     * token 前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * token 类型 claim 名
     */
    public static final String CLAIM_TYPE = "type";

    /**
     * 用户名 claim 名
     */
    public static final String CLAIM_USERNAME = "username";

    /**
     * access token 类型
     */
    public static final String TYPE_ACCESS = "access";

    /**
     * refresh token 类型
     */
    public static final String TYPE_REFRESH = "refresh";

    private AuthConstants() {
    }
}
```

### backend/demo/demo-client/src/main/java/com/fullstack/demo/client/dto/auth/RegisterReq.java（新增）

```java
package com.fullstack.demo.client.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 注册请求
 */
@Data
public class RegisterReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 登录名：4-32 位字母数字下划线
     */
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_]{4,32}$", message = "用户名须为 4-32 位字母数字下划线")
    private String username;

    /**
     * 密码：6-64 位
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度须为 6-64 位")
    private String password;

    /**
     * 昵称（可选，缺省同用户名）
     */
    @Size(max = 50, message = "昵称最长 50 字符")
    private String nickname;
}
```

### backend/demo/demo-client/src/main/java/com/fullstack/demo/client/dto/auth/LoginReq.java（新增）

```java
package com.fullstack.demo.client.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录请求
 */
@Data
public class LoginReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
```

### backend/demo/demo-client/src/main/java/com/fullstack/demo/client/dto/auth/RefreshReq.java（新增）

```java
package com.fullstack.demo.client.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 刷新 token 请求
 */
@Data
public class RefreshReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
```

### backend/demo/demo-client/src/main/java/com/fullstack/demo/client/dto/auth/TokenResp.java（新增）

```java
package com.fullstack.demo.client.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录/注册/刷新成功返回的 token 对
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 访问令牌（短效，30 分钟）
     */
    private String accessToken;

    /**
     * 刷新令牌（长效，7 天）
     */
    private String refreshToken;

    /**
     * 令牌类型（固定 Bearer）
     */
    private String tokenType;

    /**
     * access token 有效期（秒）
     */
    private Long accessExpiresIn;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 登录名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;
}
```

### backend/demo/demo-client/src/main/java/com/fullstack/demo/client/dto/user/UserInfoResp.java（新增）

```java
package com.fullstack.demo.client.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 当前用户信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private String nickname;

    /**
     * 注册时间
     */
    private LocalDateTime createTime;
}
```

### backend/demo/demo-client/src/main/java/com/fullstack/demo/client/service/TokenProvider.java（新增）

```java
package com.fullstack.demo.client.service;

import com.fullstack.demo.client.dto.auth.TokenResp;
import com.fullstack.demo.shared.auth.LoginUser;

/**
 * token 签发与解析端口。
 * <p>
 * 接口位于 client 层供 application 依赖倒置调用；
 * jjwt 实现位于 demo-infrastructure（JwtTokenProvider），由 starter 装配。
 */
public interface TokenProvider {

    /**
     * 签发 access + refresh 双 token（旋转双发）。
     *
     * @param userId   用户 ID
     * @param username 登录名
     * @param nickname 昵称
     * @return token 对
     */
    TokenResp issueToken(Long userId, String username, String nickname);

    /**
     * 解析并校验 access token（验签 + 有效期 + 类型）。
     *
     * @param token access token 原文
     * @return 登录用户上下文
     * @throws com.fullstack.common.base.exception.BusinessException 校验失败（code=401）
     */
    LoginUser parseAccess(String token);

    /**
     * 解析并校验 refresh token（验签 + 有效期 + 类型）。
     *
     * @param token refresh token 原文
     * @return 登录用户上下文
     * @throws com.fullstack.common.base.exception.BusinessException 校验失败（code=401）
     */
    LoginUser parseRefresh(String token);
}
```

### backend/demo/demo-client/src/main/java/com/fullstack/demo/client/service/AuthService.java（新增）

```java
package com.fullstack.demo.client.service;

import com.fullstack.demo.client.dto.auth.LoginReq;
import com.fullstack.demo.client.dto.auth.RegisterReq;
import com.fullstack.demo.client.dto.auth.TokenResp;

/**
 * 认证服务（实现位于 demo-application）
 */
public interface AuthService {

    /**
     * 注册并直接返回登录态（注册即登录）。
     */
    TokenResp register(RegisterReq req);

    /**
     * 用户名密码登录。
     */
    TokenResp login(LoginReq req);

    /**
     * 使用 refresh token 换取新 token 对（旋转双发）。
     */
    TokenResp refresh(String refreshToken);
}
```

### backend/demo/demo-client/src/main/java/com/fullstack/demo/client/service/UserService.java（新增）

```java
package com.fullstack.demo.client.service;

import com.fullstack.demo.client.dto.user.UserInfoResp;

/**
 * 用户服务（实现位于 demo-application）
 */
public interface UserService {

    /**
     * 查询当前登录用户信息（取自 UserContextHolder，查库取最新资料）。
     */
    UserInfoResp getCurrentUser();
}
```

## 验证

| 验证方式 | 命令或步骤 | 预期结果 |
|----------|------------|----------|
| build/test | `cd backend && mvn test` | 编译通过，全部测试绿色 |

## 风险与阻塞

- 风险：无运行时行为；DTO 字段已按冻结契约定义，后续任务不得改动字段名。
- 阻塞：无。
- 执行记录：2026-09-05 落盘 11 个类；首轮 `mvn test` 编译失败（demo-client 缺 lombok，已按修订 1 补依赖）；复验 BUILD SUCCESS。
