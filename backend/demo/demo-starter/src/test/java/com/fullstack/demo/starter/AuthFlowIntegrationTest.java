package com.fullstack.demo.starter;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 场景 01 认证全链路集成测试（黑盒 HTTP，随机 H2 + data.sql 种子数据）
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthFlowIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private String randomUsername() {
        return "u" + System.currentTimeMillis() % 100000000 + (int) (Math.random() * 100);
    }

    private Map<String, Object> post(String path, Object body, String bearer) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (bearer != null) {
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);
        }
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return rest.exchange(path, HttpMethod.POST, entity, Map.class).getBody();
    }

    private Map<String, Object> get(String path, String bearer) {
        HttpHeaders headers = new HttpHeaders();
        if (bearer != null) {
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);
        }
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), Map.class).getBody();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(Map<String, Object> body) {
        return (Map<String, Object>) body.get("data");
    }

    @Test
    @Order(1)
    void register_success_returns_tokens() {
        Map<String, Object> body = post("/auth/register",
                Map.of("username", randomUsername(), "password", "pass123456"), null);
        assertThat(body.get("code")).isEqualTo(200);
        assertThat(data(body)).isNotNull();
        assertThat(data(body).get("accessToken")).asString().isNotBlank();
        assertThat(data(body).get("refreshToken")).asString().isNotBlank();
        assertThat(data(body).get("tokenType")).isEqualTo("Bearer");
    }

    @Test
    @Order(2)
    void register_duplicate_returns_409() {
        String username = randomUsername();
        post("/auth/register", Map.of("username", username, "password", "pass123456"), null);
        Map<String, Object> body = post("/auth/register",
                Map.of("username", username, "password", "pass123456"), null);
        assertThat(body.get("code")).isEqualTo(409);
    }

    @Test
    @Order(3)
    void register_invalid_params_returns_417() {
        Map<String, Object> body = post("/auth/register",
                Map.of("username", "ab", "password", "123"), null);
        assertThat(body.get("code")).isEqualTo(417);
    }

    @Test
    @Order(4)
    void login_success_returns_tokens() {
        Map<String, Object> body = post("/auth/login",
                Map.of("username", "demo", "password", "demo123456"), null);
        assertThat(body.get("code")).isEqualTo(200);
        assertThat(data(body).get("accessToken")).asString().isNotBlank();
    }

    @Test
    @Order(5)
    void login_wrong_password_returns_401() {
        Map<String, Object> body = post("/auth/login",
                Map.of("username", "demo", "password", "wrong-password"), null);
        assertThat(body.get("code")).isEqualTo(401);
        assertThat(body.get("msg")).isEqualTo("用户名或密码错误");
    }

    @Test
    @Order(6)
    void me_without_token_returns_401() {
        Map<String, Object> body = get("/user/me", null);
        assertThat(body.get("code")).isEqualTo(401);
    }

    @Test
    @Order(7)
    void me_with_forged_token_returns_401() {
        Map<String, Object> body = get("/user/me", "abc.def.ghi");
        assertThat(body.get("code")).isEqualTo(401);
    }

    @Test
    @Order(8)
    void me_with_valid_token_returns_user() {
        Map<String, Object> login = post("/auth/login",
                Map.of("username", "demo", "password", "demo123456"), null);
        String accessToken = (String) data(login).get("accessToken");
        Map<String, Object> body = get("/user/me", accessToken);
        assertThat(body.get("code")).isEqualTo(200);
        assertThat(data(body).get("username")).isEqualTo("demo");
    }

    @Test
    @Order(9)
    void refresh_rotates_token_pair() {
        Map<String, Object> login = post("/auth/login",
                Map.of("username", "demo", "password", "demo123456"), null);
        String refreshToken = (String) data(login).get("refreshToken");
        Map<String, Object> body = post("/auth/refresh",
                Map.of("refreshToken", refreshToken), null);
        assertThat(body.get("code")).isEqualTo(200);
        assertThat(data(body).get("accessToken")).isNotEqualTo(data(login).get("accessToken"));
    }

    @Test
    @Order(10)
    void refresh_with_access_token_returns_401() {
        Map<String, Object> login = post("/auth/login",
                Map.of("username", "demo", "password", "demo123456"), null);
        String accessToken = (String) data(login).get("accessToken");
        Map<String, Object> body = post("/auth/refresh",
                Map.of("refreshToken", accessToken), null);
        assertThat(body.get("code")).isEqualTo(401);
    }
}
