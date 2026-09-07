---
id: auth-jwt_03_service_auth_core
name: "认证核心实现（JWT 签发/解析 + 应用服务 + BCrypt）"
type: service
subtype: null
status: completed
blocked_reason: null
depends: [auth-jwt_01_data_model_users_table, auth-jwt_02_contract_auth_contract]
profiles: [backend, api]
files:
  - backend/demo/demo-application/pom.xml
  - backend/demo/demo-infrastructure/pom.xml
  - backend/demo/demo-domain/src/main/java/com/fullstack/demo/domain/user/gateway/UserGateway.java
  - backend/demo/demo-infrastructure/src/main/java/com/fullstack/demo/infrastructure/gateway/UserGatewayImpl.java
  - backend/demo/demo-infrastructure/src/main/java/com/fullstack/demo/infrastructure/auth/JwtProperties.java
  - backend/demo/demo-infrastructure/src/main/java/com/fullstack/demo/infrastructure/auth/JwtTokenProvider.java
  - backend/demo/demo-application/src/main/java/com/fullstack/demo/application/config/PasswordEncoderConfig.java
  - backend/demo/demo-application/src/main/java/com/fullstack/demo/application/auth/AuthServiceImpl.java
  - backend/demo/demo-application/src/main/java/com/fullstack/demo/application/user/UserServiceImpl.java
  - backend/demo/demo-starter/src/main/resources/application.yml
  - backend/demo/demo-starter/src/test/resources/application.yml
---

# 认证核心实现（JWT 签发/解析 + 应用服务 + BCrypt）

## 任务范围

| 项目 | 内容 |
|------|------|
| 类型 | `service` |
| 已启用 profile | backend / api |
| 架构边界 | demo-infrastructure（jjwt 实现 + 配置属性）；demo-application（服务实现 + BCrypt Bean）；demo-starter（yml 配置，主/测试两份同步） |
| 结构分析 | 只读降级分析：依赖方向 application ↛ infrastructure，经 client 的 `TokenProvider` 端口反转；`scanBasePackages="com.fullstack.demo"` 使各层 `@Component` 生效 |
| 完成条件 | `mvn test` 通过；如任务 01 哈希未回填则此时生成并回填 data.sql/postgresql.sql |

## 需求与验收

- 用户目标：实现注册（重名 409、即登录）、登录（统一 401 文案）、refresh（旋转双发、无状态校验）、getMe（上下文 + 查库）。
- 包含：jjwt 签发/解析（HS256）、BCrypt 校验、`app.jwt.*` 配置。
- 不包含：Controller 与过滤器（任务 04）。
- 验收：`mvn test` 通过；登录错误统一「用户名或密码错误」；日志不落密码与 token 原文。

## 设计与依赖

| 类别 | 事实 / 决策 |
|------|-------------|
| 输入与输出 | 实现任务 02 定义的三个接口 |
| 数据/Schema | 读写 `users` 表（任务 01）；重名并发由唯一索引兜底（捕获 DuplicateKeyException 转 409） |
| 集成 | jjwt 0.12.x API（`Jwts.parser().verifyWith().build().parseSignedClaims`）；spring-security-crypto 仅取 BCrypt，无 Security 过滤链 |
| 安全与质量 | secret ≥32 字节（`Keys.hmacShaKeyFor` 强校验）；BCrypt 默认强度 10；refresh 不查库，nickname 取 username（设计已知局限） |
| 依赖 | 01（实体/Mapper/依赖版本）、02（契约）；测试 yml 遮蔽主 yml，两处必须同步加 `app.jwt` |

## 实现步骤

1. demo-application pom 加 `spring-security-crypto`；demo-infrastructure pom 加 jjwt 三件套；
2. infrastructure 新建 `auth` 包：`JwtProperties`、`JwtTokenProvider`；
3. application 新建 `config/PasswordEncoderConfig`、`auth/AuthServiceImpl`、`user/UserServiceImpl`；
4. starter 主 yml 与测试 yml 追加 `app.jwt` 配置；
5. 运行任务 01 的临时哈希生成器，回填 `data.sql` 与 `postgresql.sql`，删除临时类；
6. `mvn test` 验证。

## 涉及文件

| 文件 | 模块/区域 | 操作 | 说明 |
|------|-----------|------|------|
| `backend/demo/demo-application/pom.xml` | demo-application | 修改 | 加 spring-security-crypto |
| `backend/demo/demo-infrastructure/pom.xml` | demo-infrastructure | 修改 | 加 jjwt 三件套 |
| `backend/demo/demo-infrastructure/src/main/java/com/fullstack/demo/infrastructure/auth/JwtProperties.java` | infrastructure | 新增 | 配置属性 |
| `backend/demo/demo-infrastructure/src/main/java/com/fullstack/demo/infrastructure/auth/JwtTokenProvider.java` | infrastructure | 新增 | jjwt 实现 |
| `backend/demo/demo-application/src/main/java/com/fullstack/demo/application/config/PasswordEncoderConfig.java` | application | 新增 | BCrypt Bean |
| `backend/demo/demo-application/src/main/java/com/fullstack/demo/application/auth/AuthServiceImpl.java` | application | 新增 | 注册/登录/刷新 |
| `backend/demo/demo-application/src/main/java/com/fullstack/demo/application/user/UserServiceImpl.java` | application | 新增 | 当前用户 |
| `backend/demo/demo-starter/src/main/resources/application.yml` | starter | 修改 | 追加 app.jwt |
| `backend/demo/demo-starter/src/test/resources/application.yml` | starter test | 修改 | 追加 app.jwt |

## 完整代码（供手动敲写）

### backend/demo/demo-infrastructure/pom.xml（修改，追加二）

```xml
<!-- dependencies 段内追加（demo-domain 之后）：JwtTokenProvider 实现 client 层 TokenProvider 端口需要契约可见 -->
<dependency>
    <groupId>com.fullstack</groupId>
    <artifactId>demo-client</artifactId>
</dependency>
```

修改点说明：执行期发现 demo-infrastructure 原依赖只有 demo-domain，编译找不到 client 契约；按 demo-client README 的「Gateway 实现调用 client/feign 契约」同一模式补充，无循环依赖。

修订：2 - 补 demo-client 依赖（infrastructure 实现 client 端口需要契约可见）。

### backend/demo/demo-application/pom.xml（修改）

```xml
<!-- dependencies 段内追加（lombok 之前）；版本由 Spring Boot BOM 管理 -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```

修改点说明：仅引入 crypto 轻模块（BCryptPasswordEncoder），不带 Spring Security 过滤链与自动配置。落点：`<dependencies>` 内。

### backend/demo/demo-infrastructure/pom.xml（修改）

```xml
<!-- dependencies 段内追加；版本由根 pom dependencyManagement 管理 -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <scope>runtime</scope>
</dependency>
```

修改点说明：api 编译期可见，impl/jackson 仅运行期。落点：`<dependencies>` 内。

### backend/demo/demo-infrastructure/src/main/java/com/fullstack/demo/infrastructure/auth/JwtProperties.java（新增）

```java
package com.fullstack.demo.infrastructure.auth;

import io.jsonwebtoken.security.Keys;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT 配置（app.jwt.*）
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * HS256 签名密钥（≥ 32 字节；生产环境必须替换并通过环境变量注入）
     */
    private String secret;

    /**
     * access token 有效期（秒），默认 30 分钟
     */
    private long accessExpireSeconds = 1800;

    /**
     * refresh token 有效期（秒），默认 7 天
     */
    private long refreshExpireSeconds = 604800;

    /**
     * 构造签名密钥（长度不足 32 字节时 hmacShaKeyFor 会抛异常，启动即暴露配置问题）。
     */
    public SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
```

### backend/demo/demo-infrastructure/src/main/java/com/fullstack/demo/infrastructure/auth/JwtTokenProvider.java（新增）

```java
package com.fullstack.demo.infrastructure.auth;

import com.fullstack.common.base.enums.ResultCodeEnum;
import com.fullstack.common.base.exception.BusinessException;
import com.fullstack.demo.client.dto.auth.TokenResp;
import com.fullstack.demo.client.service.TokenProvider;
import com.fullstack.demo.shared.auth.AuthConstants;
import com.fullstack.demo.shared.auth.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * jjwt 实现的 token 签发与解析（HS256）。
 * <p>
 * 实现 client 层 TokenProvider 端口，由组件扫描装配，application 层经接口调用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider implements TokenProvider {

    private final JwtProperties jwtProperties;

    @Override
    public TokenResp issueToken(Long userId, String username, String nickname) {
        long now = System.currentTimeMillis();
        String accessToken = buildToken(userId, username, AuthConstants.TYPE_ACCESS,
                now, jwtProperties.getAccessExpireSeconds());
        String refreshToken = buildToken(userId, username, AuthConstants.TYPE_REFRESH,
                now, jwtProperties.getRefreshExpireSeconds());
        return TokenResp.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessExpiresIn(jwtProperties.getAccessExpireSeconds())
                .userId(userId)
                .username(username)
                .nickname(nickname)
                .build();
    }

    @Override
    public LoginUser parseAccess(String token) {
        return parse(token, AuthConstants.TYPE_ACCESS);
    }

    @Override
    public LoginUser parseRefresh(String token) {
        return parse(token, AuthConstants.TYPE_REFRESH);
    }

    private String buildToken(Long userId, String username, String type, long now, long expireSeconds) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(AuthConstants.CLAIM_USERNAME, username)
                .claim(AuthConstants.CLAIM_TYPE, type)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expireSeconds * 1000))
                .signWith(jwtProperties.getSecretKey())
                .compact();
    }

    private LoginUser parse(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtProperties.getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String type = claims.get(AuthConstants.CLAIM_TYPE, String.class);
            if (!expectedType.equals(type)) {
                throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "token 类型不正确");
            }
            return new LoginUser(Long.valueOf(claims.getSubject()),
                    claims.get(AuthConstants.CLAIM_USERNAME, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            // 只记录异常摘要，不落 token 原文
            log.warn("JWT 解析失败：{}", e.getMessage());
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED);
        }
    }
}
```

### backend/demo/demo-application/src/main/java/com/fullstack/demo/application/config/PasswordEncoderConfig.java（新增）

```java
package com.fullstack.demo.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码编码器配置（BCrypt，默认强度 10）
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### backend/demo/demo-domain/src/main/java/com/fullstack/demo/domain/user/gateway/UserGateway.java（新增）

```java
package com.fullstack.demo.domain.user.gateway;

import com.fullstack.demo.domain.user.User;

/**
 * 用户领域网关（仓储接口）。
 * <p>
 * 接口位于 domain，实现位于 demo-infrastructure（UserGatewayImpl，委托 UserMapper）；
 * application 层只依赖本接口，不触碰 MyBatis 细节。
 */
public interface UserGateway {

    /**
     * 按登录名查询（含逻辑删除过滤）。
     */
    User findByUsername(String username);

    /**
     * 按主键查询。
     */
    User findById(Long id);

    /**
     * 新增用户（回填雪花 ID）。
     */
    void insert(User user);
}
```

### backend/demo/demo-infrastructure/src/main/java/com/fullstack/demo/infrastructure/gateway/UserGatewayImpl.java（新增）

```java
package com.fullstack.demo.infrastructure.gateway;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fullstack.demo.domain.user.User;
import com.fullstack.demo.domain.user.gateway.UserGateway;
import com.fullstack.demo.infrastructure.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 用户网关实现：委托 MyBatis-Plus Mapper。
 */
@Component
@RequiredArgsConstructor
public class UserGatewayImpl implements UserGateway {

    private final UserMapper userMapper;

    @Override
    public User findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public void insert(User user) {
        userMapper.insert(user);
    }
}
```

### backend/demo/demo-application/src/main/java/com/fullstack/demo/application/auth/AuthServiceImpl.java（新增）

```java
package com.fullstack.demo.application.auth;

import com.fullstack.common.base.enums.ResultCodeEnum;
import com.fullstack.common.base.exception.BusinessException;
import com.fullstack.demo.client.dto.auth.LoginReq;
import com.fullstack.demo.client.dto.auth.RegisterReq;
import com.fullstack.demo.client.dto.auth.TokenResp;
import com.fullstack.demo.client.service.AuthService;
import com.fullstack.demo.client.service.TokenProvider;
import com.fullstack.demo.domain.user.User;
import com.fullstack.demo.domain.user.gateway.UserGateway;
import com.fullstack.demo.shared.auth.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 认证服务实现：注册 / 登录 / 刷新
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserGateway userGateway;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    @Override
    public TokenResp register(RegisterReq req) {
        String username = req.getUsername();
        User exist = userGateway.findByUsername(username);
        if (exist != null) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "用户名已被注册");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(StringUtils.hasText(req.getNickname()) ? req.getNickname() : username);
        try {
            userGateway.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发重名由唯一索引兜底
            throw new BusinessException(ResultCodeEnum.CONFLICT, "用户名已被注册");
        }
        log.info("用户注册成功：{}", username);
        return tokenProvider.issueToken(user.getId(), username, user.getNickname());
    }

    @Override
    public TokenResp login(LoginReq req) {
        User user = userGateway.findByUsername(req.getUsername());
        // 用户不存在与密码错误使用同一文案，避免用户名枚举
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "用户名或密码错误");
        }
        log.info("用户登录成功：{}", user.getUsername());
        return tokenProvider.issueToken(user.getId(), user.getUsername(), user.getNickname());
    }

    @Override
    public TokenResp refresh(String refreshToken) {
        // 无状态校验：验签 + 有效期 + 类型，不查库（设计已知局限：昵称取不到最新值）
        LoginUser login = tokenProvider.parseRefresh(refreshToken);
        return tokenProvider.issueToken(login.getUserId(), login.getUsername(), login.getUsername());
    }
}

修订：3 - 执行期发现 application 层按 AGENTS 依赖规则不可见 infrastructure（UserMapper 落位于此），引入 domain 的 UserGateway 端口 + infrastructure 的 UserGatewayImpl 实现，application 改依赖网关接口（与「infrastructure 的 Gateway 实现」架构表述一致）。
```

### backend/demo/demo-application/src/main/java/com/fullstack/demo/application/user/UserServiceImpl.java（新增）

```java
package com.fullstack.demo.application.user;

import com.fullstack.common.base.enums.ResultCodeEnum;
import com.fullstack.common.base.exception.BusinessException;
import com.fullstack.demo.client.dto.user.UserInfoResp;
import com.fullstack.demo.client.service.UserService;
import com.fullstack.demo.domain.user.User;
import com.fullstack.demo.domain.user.gateway.UserGateway;
import com.fullstack.demo.shared.auth.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserGateway userGateway;

    @Override
    public UserInfoResp getCurrentUser() {
        // 过滤器保证上下文存在，此处兜底防御
        if (UserContextHolder.get() == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED);
        }
        User user = userGateway.findById(UserContextHolder.get().getUserId());
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "用户不存在或已注销");
        }
        return new UserInfoResp(user.getId(), user.getUsername(), user.getNickname(), user.getCreateTime());
    }
}

修订：3 - 同 AuthServiceImpl，UserMapper 直注改为 UserGateway（见修订 3 说明）。
```

### backend/demo/demo-starter/src/main/resources/application.yml（修改）

```yaml
app:
  jwt:
    # HS256 密钥，须 ≥ 32 字节；生产环境必须替换并通过环境变量注入
    secret: demo-jwt-secret-0123456789abcdef-change-me
    access-expire-seconds: 1800
    refresh-expire-seconds: 604800
  cors:
    allowed-origins: http://localhost:5173,http://localhost:5174,http://localhost:3000
```

修改点说明：在既有 `app:` 段下新增 `jwt:` 子段（`cors` 保持不变）。落点：文件末尾 `app` 块。

### backend/demo/demo-starter/src/test/resources/application.yml（修改）

```yaml
app:
  jwt:
    secret: demo-jwt-secret-0123456789abcdef-change-me
    access-expire-seconds: 1800
    refresh-expire-seconds: 604800
  cors:
    allowed-origins: http://localhost:5173
```

修改点说明：测试配置同名遮蔽主配置，必须同步追加 `app.jwt`，否则测试上下文启动失败。落点：文件末尾 `app` 块。

## 验证

| 验证方式 | 命令或步骤 | 预期结果 |
|----------|------------|----------|
| build/test | `cd backend && mvn test` | 全部通过（含 contextLoads，验证 Bean 装配与配置绑定） |
| manual | 观察启动日志 | 无 `app.jwt` 绑定报错、无密钥长度异常 |

## 风险与阻塞

- 风险：测试 yml 漏加 `app.jwt` 会导致所有 `@SpringBootTest` 失败（遮蔽机制）；jjwt 0.12.6 若 Maven Central 不可得需调整 patch 版本（API 不变）。
- 阻塞：无。
- 执行记录：2026-09-05 全部落盘并回填 BCrypt 哈希（临时生成器已删除）；执行期三项修订——①jjwt 用本地仓库已有 0.13.0（内网镜像不可达，任务 01 修订 1）②infrastructure 补 demo-client 依赖以实现 TokenProvider 端口③application 不可见 infrastructure，引入 UserGateway 端口（domain）+ UserGatewayImpl（infrastructure）；`mvn test` BUILD SUCCESS。
- 环境备注：本机 Maven 双镜像指向内网私服 cehui（192.168.102.156:8181，当前不可达），真实本地仓库为 `D:\Environment\Repository`；spring-security-crypto 6.5.11 经临时 settings 从 Maven Central 下载后拷入该仓库。

修订：4 - 任务 05 集成测试发现同一秒内签发的 token 字节级相同（iat/exp 秒级精度，claims 全等 → HMAC 签名全等），`buildToken` 增加 `.id(UUID.randomUUID())`（jti claim）保证唯一性，亦为未来黑名单留扩展点。
