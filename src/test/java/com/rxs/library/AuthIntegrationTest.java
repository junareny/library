package com.rxs.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 认证与访问控制集成测试。 */
class AuthIntegrationTest extends AbstractLibraryIntegrationTest {
    @Test
    @DisplayName("首页和静态资源公开，借阅接口需要认证")
    void frontendIsPublicWhileLoansRemainProtected() throws Exception {
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("id=\"main-content\"")));
        mvc.perform(get("/assets/app.css")).andExpect(status().isOk());
        mvc.perform(get("/assets/app.js")).andExpect(status().isOk());
        mvc.perform(postJson("/ILoanBiz/queryCurrentLoan/page", "{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("注册登录与密码哈希")
    void authenticationAndPasswordHashing() throws Exception {
        mvc.perform(
                        post("/IAuthBiz/register")
                                .contentType("application/json")
                                .content("{\"username\":\"alice\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("body.username").value("alice"))
                .andExpect(jsonPath("body.passwordHash").doesNotExist());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT password_hash FROM users WHERE username='alice'",
                                String.class))
                .startsWith("$2")
                .isNotEqualTo("password123");
        mvc.perform(
                        post("/IAuthBiz/register")
                                .contentType("application/json")
                                .content("{\"username\":\"alice\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("code").value("USERNAME_EXISTS"));
        mvc.perform(
                        post("/IAuthBiz/login")
                                .contentType("application/json")
                                .content("{\"username\":\"alice\",\"password\":\"incorrect\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("code").value("UNAUTHORIZED"));
        String token = login("alice");
        mvc.perform(
                        postJson("/ILoanBiz/queryCurrentLoan/page", "{}")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("body.total").value(0));
    }

    @Test
    @DisplayName("拒绝匿名请求及无效或过期令牌")
    void rejectsAnonymousMalformedExpiredAndFutureTokens() throws Exception {
        long uid = user("alice");
        mvc.perform(postJson("/ILoanBiz/queryCurrentLoan/page", "{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("code").value("UNAUTHORIZED"));
        mvc.perform(
                        postJson("/ILoanBiz/queryCurrentLoan/page", "{}")
                                .header("Authorization", "Bearer broken"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("code").value("UNAUTHORIZED"));
        for (String token :
                List.of(
                        token(uid, NOW.minusSeconds(7200), NOW.minusSeconds(1), "library-api"),
                        token(uid, NOW.plusSeconds(60), NOW.plusSeconds(7200), "library-api"),
                        token(uid, NOW, NOW.plusSeconds(7200), "wrong-issuer"))) {
            mvc.perform(
                            postJson("/ILoanBiz/queryCurrentLoan/page", "{}")
                                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("拒绝非法输入及客户端指定角色")
    void rejectsInvalidInputsAndClientAssignedRoles() throws Exception {
        for (String body :
                List.of(
                        "{\"username\":\"alice\",\"password\":\"short\"}",
                        "{\"username\":\"a b\",\"password\":\"password123\"}",
                        "{\"username\":\"alice\",\"password\":\"password123\",\"role\":\"ADMIN\"}",
                        json.writeValueAsString(
                                Map.of("username", "alice", "password", "密".repeat(25))))) {
            mvc.perform(post("/IAuthBiz/register").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("code").value("VALIDATION_ERROR"));
        }
        for (String query :
                List.of(
                        "{\"pageCurrent\":0}",
                        "{\"pageSize\":0}",
                        "{\"pageSize\":101}",
                        "{\"pageCurrent\":\"abc\"}")) {
            mvc.perform(postJson("/IBookBiz/queryBook/page", query))
                    .andExpect(status().isBadRequest());
        }
        long uid = user("alice");
        mvc.perform(
                        post("/ILoanBiz/borrowBook")
                                .header(
                                        "Authorization",
                                        "Bearer "
                                                + token(
                                                        uid,
                                                        NOW,
                                                        NOW.plusSeconds(7200),
                                                        "library-api"))
                                .contentType("application/json")
                                .content("{\"bookId\":1,\"userId\":999}"))
                .andExpect(status().isBadRequest());
    }
}
