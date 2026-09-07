package com.fullstack.demo.starter;

import com.fullstack.demo.client.dto.auth.TokenResp;
import com.fullstack.demo.client.service.TokenProvider;
import com.fullstack.demo.domain.user.User;
import com.fullstack.demo.domain.user.gateway.UserGateway;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 场景 03 秒杀集成测试（黑盒 HTTP，随机 H2 + data.sql 种子）。
 * <p>
 * 并发正确性断言的是不变式而非单次结果：任意并发下
 * 成功订单数 + 剩余库存 == 初始库存，且成功订单用户互不相同。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlashSaleIntegrationTest {

    /** 并发用户数（= 线程数，Tomcat 默认 200 工作线程内） */
    private static final int CONCURRENT_USERS = 200;

    /** 单用户并发重复提交线程数 */
    private static final int DUPLICATE_THREADS = 20;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private UserGateway userGateway;

    @Autowired
    private TokenProvider tokenProvider;

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
    private List<Map<String, Object>> dataList(Map<String, Object> body) {
        return (List<Map<String, Object>>) body.get("data");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(Map<String, Object> body) {
        return (Map<String, Object>) body.get("data");
    }

    private String loginDemo() {
        Map<String, Object> body = post("/auth/login",
                Map.of("username", "demo", "password", "demo123456"), null);
        return (String) data(body).get("accessToken");
    }

    /**
     * 直接插入测试用户并签发 access token：绕开注册接口的 BCrypt（约 80ms/次），
     * 200 个用户的构造保持毫秒级。
     */
    private String tokenForNewUser(String suffix) {
        User user = new User();
        user.setUsername("flash-" + System.nanoTime() + "-" + suffix);
        user.setPassword("$2a$10$placeholder-not-for-login");
        user.setNickname("并发测试用户");
        userGateway.insert(user);
        TokenResp token = tokenProvider.issueToken(user.getId(), user.getUsername(), user.getNickname());
        return token.getAccessToken();
    }

    @Test
    @Order(1)
    void flash_without_token_returns_401() {
        Map<String, Object> body = get("/flash/items", null);
        assertThat(body.get("code")).isEqualTo(401);
    }

    @Test
    @Order(2)
    void list_items_returns_seeded_states() {
        List<Map<String, Object>> items = dataList(get("/flash/items", loginDemo()));
        assertThat(items).hasSize(4);
        // 种子固定：1/2 进行中、3 未开始、4 已结束；demo 用户尚未抢购
        assertThat(items).anySatisfy(i -> {
            assertThat(i.get("id")).isEqualTo(1);
            assertThat(i.get("status")).isEqualTo("IN_PROGRESS");
            assertThat(i.get("mine")).isEqualTo(false);
        });
        assertThat(items).anySatisfy(i -> {
            assertThat(i.get("id")).isEqualTo(3);
            assertThat(i.get("status")).isEqualTo("NOT_STARTED");
        });
        assertThat(items).anySatisfy(i -> {
            assertThat(i.get("id")).isEqualTo(4);
            assertThat(i.get("status")).isEqualTo("ENDED");
        });
    }

    @Test
    @Order(3)
    void seckill_not_started_returns_6101() {
        Map<String, Object> body = post("/flash/items/3/seckill", Map.of(), loginDemo());
        assertThat(body.get("code")).isEqualTo(6101);
    }

    @Test
    @Order(4)
    void seckill_ended_returns_6102() {
        Map<String, Object> body = post("/flash/items/4/seckill", Map.of(), loginDemo());
        assertThat(body.get("code")).isEqualTo(6102);
    }

    @Test
    @Order(5)
    void seckill_nonexistent_returns_404() {
        Map<String, Object> body = post("/flash/items/999/seckill", Map.of(), loginDemo());
        assertThat(body.get("code")).isEqualTo(404);
    }

    @Test
    @Order(6)
    void seckill_success_then_duplicate_returns_6104() {
        String token = loginDemo();
        Map<String, Object> first = post("/flash/items/1/seckill", Map.of(), token);
        assertThat(first.get("code")).isEqualTo(200);
        assertThat(data(first)).isNotNull();
        assertThat(data(first).get("orderId")).isNotNull();

        Map<String, Object> second = post("/flash/items/1/seckill", Map.of(), token);
        assertThat(second.get("code")).isEqualTo(6104);

        // 库存 50 -> 49，且列表标记 mine=true
        List<Map<String, Object>> items = dataList(get("/flash/items", token));
        Map<String, Object> item1 = items.stream().filter(i -> i.get("id").equals(1)).findFirst().orElseThrow();
        assertThat(item1.get("stock")).isEqualTo(49);
        assertThat(item1.get("mine")).isEqualTo(true);
    }

    @Test
    @Order(7)
    void my_orders_contains_success_order() {
        List<Map<String, Object>> orders = dataList(get("/flash/orders/mine", loginDemo()));
        assertThat(orders).isNotEmpty();
        assertThat(orders.get(0).get("itemId")).isEqualTo(1);
        assertThat(orders.get(0).get("itemName")).isEqualTo("旗舰手机 Pro");
    }

    @Test
    @Order(8)
    void concurrent_users_never_oversell() throws Exception {
        // 商品 2：库存 5；CONCURRENT_USERS 个互不相同用户同时抢
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch ready = new CountDownLatch(CONCURRENT_USERS);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        ConcurrentHashMap<String, Integer> failureCodes = new ConcurrentHashMap<>();
        try {
            IntStream.range(0, CONCURRENT_USERS).forEach(i -> pool.submit(() -> {
                String token = tokenForNewUser(String.valueOf(i));
                ready.countDown();
                try {
                    start.await();
                    Map<String, Object> body = post("/flash/items/2/seckill", Map.of(), token);
                    int code = ((Number) body.get("code")).intValue();
                    if (code == 200) {
                        success.incrementAndGet();
                    } else {
                        failureCodes.merge(String.valueOf(code), 1, Integer::sum);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
            assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
            start.countDown();
        } finally {
            pool.shutdown();
        }
        // awaitTermination 必须在 shutdown() 之后调用，否则立即返回 false
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        // 不变式：成功订单数恰为初始库存 5；其余全部 6103（互不相同用户不存在 6104）
        assertThat(success.get()).isEqualTo(5);
        assertThat(failureCodes).containsOnly(Map.entry("6103", CONCURRENT_USERS - 5));

        // 终态库存为 0（对账：订单数 + 库存 == 初始库存）
        List<Map<String, Object>> items = dataList(get("/flash/items", loginDemo()));
        Map<String, Object> item2 = items.stream().filter(i -> i.get("id").equals(2)).findFirst().orElseThrow();
        assertThat(item2.get("stock")).isEqualTo(0);
    }

    @Test
    @Order(9)
    void concurrent_duplicate_submissions_create_single_order() throws Exception {
        // 同一用户 DUPLICATE_THREADS 线程并发抢购同一商品：唯一索引保证恰好 1 单
        String token = tokenForNewUser("dup");
        ExecutorService pool = Executors.newFixedThreadPool(DUPLICATE_THREADS);
        CountDownLatch ready = new CountDownLatch(DUPLICATE_THREADS);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger duplicate = new AtomicInteger();
        try {
            IntStream.range(0, DUPLICATE_THREADS).forEach(i -> pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    Map<String, Object> body = post("/flash/items/1/seckill", Map.of(), token);
                    int code = ((Number) body.get("code")).intValue();
                    if (code == 200) {
                        success.incrementAndGet();
                    } else if (code == 6104) {
                        duplicate.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
            assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
            start.countDown();
        } finally {
            pool.shutdown();
        }
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(success.get()).isEqualTo(1);
        assertThat(duplicate.get()).isEqualTo(DUPLICATE_THREADS - 1);
    }

    @Test
    @Order(10)
    void demo_reset_restores_stock_and_frees_limit() {
        // Order(8) 已把商品 2 抢空并打上售罄标记——重置应全部复原
        String token = tokenForNewUser("rst");
        assertThat(post("/flash/items/2/seckill", Map.of(), token).get("code")).isEqualTo(6103);

        Map<String, Object> reset = post("/flash/demo/reset/2", Map.of(), token);
        assertThat(reset.get("code")).isEqualTo(200);

        // 同一用户首抢成功（库存回满 + 唯一索引已物理释放 + 售罄标记已清除）、二抢 6104
        assertThat(post("/flash/items/2/seckill", Map.of(), token).get("code")).isEqualTo(200);
        assertThat(post("/flash/items/2/seckill", Map.of(), token).get("code")).isEqualTo(6104);

        List<Map<String, Object>> items = dataList(get("/flash/items", loginDemo()));
        Map<String, Object> item2 = items.stream().filter(i -> i.get("id").equals(2)).findFirst().orElseThrow();
        assertThat(item2.get("stock")).isEqualTo(4);
    }

    @Test
    @Order(11)
    void demo_reset_ended_item_rejected() {
        Map<String, Object> body = post("/flash/demo/reset/4", Map.of(), loginDemo());
        assertThat(body.get("code")).isEqualTo(417);
    }
}
