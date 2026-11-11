package com.rxs.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.rxs.library.exception.LibraryErrorCode;
import com.rxs.library.security.ratelimit.RateLimitProperties;

/** 通过真实安全链和 MVC 链验证限流响应、身份隔离及业务无副作用。 */
@TestPropertySource(
        properties = {
            "app.rate-limit.enabled=true",
            "app.rate-limit.login.capacity=2",
            "app.rate-limit.login.period=1h",
            "app.rate-limit.register.capacity=2",
            "app.rate-limit.register.period=1h",
            "app.rate-limit.loan.capacity=2",
            "app.rate-limit.loan.period=1h"
        })
class RateLimitIntegrationTest extends AbstractLibraryIntegrationTest {
    @Autowired private RateLimitProperties rateLimitProperties;

    @Test
    @DisplayName("登录超限返回中英文消息、429 状态及重试时间")
    void loginReturnsLocalizedRateLimitResponse() throws Exception {
        assertThat(rateLimitProperties.isEnabled()).isTrue();
        String body = "{\"username\":\"unknown\",\"password\":\"password123\"}";
        for (int i = 0; i < 2; i++) {
            mvc.perform(fromIp(postJson("/IAuthBiz/login", body), "192.0.2.1"))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(
                        fromIp(postJson("/IAuthBiz/login", body), "192.0.2.1")
                                .header("Accept-Language", "en"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().string("Content-Language", "en"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("code").value(LibraryErrorCode.TOO_MANY_REQUESTS.getCode()))
                .andExpect(jsonPath("msg").value("Too many requests. Please try again later."))
                .andExpect(jsonPath("body").isEmpty());
        mvc.perform(fromIp(postJson("/IAuthBiz/login", body), "192.0.2.1"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("msg").value("请求过于频繁，请稍后再试。"));
    }

    @Test
    @DisplayName("注册按直连 IP 限流且不能通过伪造转发头绕过")
    void registrationUsesRemoteAddressAndLeavesPublicResourcesAvailable() throws Exception {
        for (int i = 0; i < 2; i++) {
            mvc.perform(fromIp(postJson("/IAuthBiz/register", "{}"), "192.0.2.2"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(
                        fromIp(postJson("/IAuthBiz/register", "{}"), "192.0.2.2")
                                .header("X-Forwarded-For", "192.0.2.99")
                                .header("Forwarded", "for=192.0.2.99"))
                .andExpect(status().isTooManyRequests());
        mvc.perform(fromIp(postJson("/IAuthBiz/register", "{}"), "192.0.2.3"))
                .andExpect(status().isBadRequest());
        mvc.perform(fromIp(postJson("/IAuthBiz/login", "{}"), "192.0.2.2"))
                .andExpect(status().isBadRequest());
        mvc.perform(fromIp(postJson("/IBookBiz/queryBook/page", "{}"), "192.0.2.2"))
                .andExpect(status().isOk());
        mvc.perform(get("/index.html")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("借还共享用户额度，换令牌不能绕过且超限不修改库存")
    void loanLimitUsesVerifiedUserAndRejectsBeforeTransaction() throws Exception {
        long first = user("rate_first");
        long second = user("rate_second");
        String firstToken = token(first, NOW, NOW.plusSeconds(7200), "library-api");
        var result =
                mvc.perform(
                                postJson("/ILoanBiz/borrowBook", "{\"bookId\":1}")
                                        .header("Authorization", "Bearer " + firstToken))
                        .andExpect(status().isOk())
                        .andReturn();
        long loanId =
                json.readTree(result.getResponse().getContentAsString())
                        .path("body")
                        .path("id")
                        .asLong();
        mvc.perform(
                        postJson("/ILoanBiz/returnBook", "{\"id\":" + loanId + "}")
                                .header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk());
        String refreshed = token(first, NOW.minusSeconds(1), NOW.plusSeconds(7100), "library-api");
        mvc.perform(
                        postJson("/ILoanBiz/borrowBook", "{\"bookId\":1}")
                                .header("Authorization", "Bearer " + refreshed))
                .andExpect(status().isTooManyRequests());
        assertThat(loans.countActive(first)).isZero();
        mvc.perform(
                        postJson("/ILoanBiz/borrowBook", "{\"bookId\":1}")
                                .header(
                                        "Authorization",
                                        "Bearer "
                                                + token(
                                                        second,
                                                        NOW,
                                                        NOW.plusSeconds(7200),
                                                        "library-api")))
                .andExpect(status().isOk());
        invariant();
    }

    @Test
    @DisplayName("未认证请求仍返回 401，不进入业务限流")
    void authenticationPrecedesLoanRateLimiting() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(postJson("/ILoanBiz/borrowBook", "{\"bookId\":1}"))
                    .andExpect(status().isUnauthorized());
        }
        long uid = user("rate_auth");
        mvc.perform(
                        postJson("/ILoanBiz/borrowBook", "{\"bookId\":1}")
                                .header(
                                        "Authorization",
                                        "Bearer "
                                                + token(
                                                        uid,
                                                        NOW,
                                                        NOW.plusSeconds(7200),
                                                        "library-api")))
                .andExpect(status().isOk());
        invariant();
    }

    @Test
    @DisplayName("设置应用上下文路径后限流仍按实际接口路由匹配")
    void contextPathDoesNotBypassRateLimit() throws Exception {
        for (int i = 0; i < 2; i++) {
            mvc.perform(
                            fromIp(
                                    post("/library/IAuthBiz/login")
                                            .contextPath("/library")
                                            .contentType("application/json")
                                            .content("{}"),
                                    "192.0.2.4"))
                    .andExpect(status().isBadRequest());
        }
        var result =
                mvc.perform(
                                fromIp(
                                        post("/library/IAuthBiz/login")
                                                .contextPath("/library")
                                                .contentType("application/json")
                                                .content("{}"),
                                        "192.0.2.4"))
                        .andExpect(status().isTooManyRequests())
                        .andReturn();
        assertThat(Long.parseLong(result.getResponse().getHeader("Retry-After")))
                .isBetween(1L, 1800L);
    }

    private MockHttpServletRequestBuilder fromIp(MockHttpServletRequestBuilder request, String ip) {
        return request.with(
                servletRequest -> {
                    servletRequest.setRemoteAddr(ip);
                    return servletRequest;
                });
    }
}
