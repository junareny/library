package com.rxs.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rxs.library.auth.application.service.AuthAppService;
import com.rxs.library.auth.domain.bo.CredentialsBO;
import com.rxs.library.book.domain.service.IBookTableService;
import com.rxs.library.common.ApiException;
import com.rxs.library.exception.LibraryErrorCode;
import com.rxs.library.loan.application.service.LoanAppService;
import com.rxs.library.loan.domain.service.ILoanTableService;
import com.rxs.library.user.domain.service.IUserTableService;

/** 共享测试上下文和数据清理逻辑，仅允许操作专用 MySQL 5.7 测试库。 */
@SpringBootTest(
        properties = {
            "spring.config.import=optional:file:./.local/test-database.properties",
            "spring.datasource.url=${TEST_DB_URL}",
            "spring.datasource.username=${TEST_DB_USERNAME}",
            "spring.datasource.password=${TEST_DB_PASSWORD}",
            "app.jwt-secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        })
@AutoConfigureMockMvc
@Import(AbstractLibraryIntegrationTest.TimeConfig.class)
abstract class AbstractLibraryIntegrationTest {
    protected static final Instant NOW = Instant.parse("2026-11-09T04:00:00Z");

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected AuthAppService auth;
    @Autowired protected LoanAppService service;
    @Autowired protected ILoanTableService loans;
    @Autowired protected IBookTableService books;
    @Autowired protected PlatformTransactionManager txManager;
    @Autowired protected JwtEncoder encoder;
    @Autowired protected TestClock clock;
    @Autowired protected IUserTableService users;

    @BeforeEach
    void clean() {
        // 清理数据前必须确认连接专用 MySQL 5.7 测试库，校验失败时立即终止。
        assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class)).endsWith("_test");
        assertThat(jdbc.queryForObject("SELECT VERSION()", String.class)).startsWith("5.7.");
        jdbc.execute("DROP TRIGGER IF EXISTS test_reject_loan");
        jdbc.update("DELETE FROM loans");
        jdbc.update("DELETE FROM users");
        jdbc.update("UPDATE books SET available_copies = total_copies");
        clock.set(NOW);
    }

    protected final MockHttpServletRequestBuilder postJson(String path, String body) {
        return post(path).contentType("application/json").content(body);
    }

    protected final long user(String name) {
        return auth.register(new CredentialsBO(name, "password123")).id();
    }

    protected final String login(String name) throws Exception {
        MvcResult result =
                mvc.perform(
                                post("/IAuthBiz/login")
                                        .contentType("application/json")
                                        .content(
                                                json.writeValueAsString(
                                                        new CredentialsBO(name, "password123"))))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("body.expiresIn").value(7200))
                        .andReturn();
        return json.readTree(result.getResponse().getContentAsString())
                .get("body")
                .get("accessToken")
                .asText();
    }

    protected final String token(long uid, Instant issued, Instant expires, String issuer) {
        return encoder.encode(
                        JwtEncoderParameters.from(
                                JwsHeader.with(MacAlgorithm.HS256).build(),
                                JwtClaimsSet.builder()
                                        .subject(Long.toString(uid))
                                        .issuer(issuer)
                                        .issuedAt(issued)
                                        .expiresAt(expires)
                                        .build()))
                .getTokenValue();
    }

    protected final void invariant() {
        assertThat(
                        jdbc.queryForObject(
                                """
                                SELECT COUNT(*) FROM books b
                                WHERE b.total_copies <> b.available_copies
                                  + (SELECT COUNT(*) FROM loans l
                                     WHERE l.book_id=b.id AND l.returned_at IS NULL)
                                """,
                                Long.class))
                .isZero();
    }

    protected final void assertCode(Runnable action, LibraryErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        ApiException.class, ex -> assertThat(ex.errorCode()).isSameAs(errorCode));
    }

    protected final String borrowResult(long uid, long book) {
        try {
            service.borrow(uid, book);
            return "OK";
        } catch (ApiException ex) {
            return ex.code();
        }
    }

    protected final String returnResult(long uid, long loan) {
        try {
            service.returnLoan(uid, loan);
            return "OK";
        } catch (ApiException ex) {
            return ex.code();
        }
    }

    protected final List<String> race(Supplier<String> a, Supplier<String> b) throws Exception {
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);
            List<Future<String>> futures = new ArrayList<>();
            for (Supplier<String> action : List.of(a, b)) {
                futures.add(
                        pool.submit(
                                () -> {
                                    ready.countDown();
                                    if (!go.await(10, TimeUnit.SECONDS)) {
                                        throw new IllegalStateException("并发测试等待启动超时");
                                    }
                                    return action.get();
                                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            List<String> results = new ArrayList<>();
            for (Future<String> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        }
    }

    @TestConfiguration
    static class TimeConfig {
        @Bean
        FailureController failureController() {
            return new FailureController();
        }

        @Bean
        @Primary
        TestClock testClock() {
            return new TestClock();
        }
    }

    /** 仅在测试上下文注册，覆盖异常兜底路径，不增加生产接口。 */
    @TestComponent
    @RestController
    static class FailureController {
        @PostMapping("/test/errors/{kind}")
        public Object fail(@PathVariable String kind) {
            if ("forbidden".equals(kind)) {
                throw new ApiException(LibraryErrorCode.FORBIDDEN);
            }
            if ("unavailable".equals(kind)) {
                throw new DataAccessResourceFailureException("internal-database-detail");
            }
            throw new IllegalStateException("internal-database-detail");
        }
    }

    static class TestClock extends Clock {
        private volatile Instant now = NOW;

        void set(Instant now) {
            this.now = now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
