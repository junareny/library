package com.rxs.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** 接口中英文消息与语言隔离集成测试。 */
class InternationalizationIntegrationTest extends AbstractLibraryIntegrationTest {
    @Test
    @DisplayName("语言协商与成功消息")
    void languageNegotiationAndSuccessMessages() throws Exception {
        for (String header : List.of("en", "en-US", "en-GB", "fr;q=1,en;q=0.8,zh-CN;q=0.5")) {
            mvc.perform(
                            postJson("/IBookBiz/queryBook/page", "{}")
                                    .header("Accept-Language", header))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Language", "en"))
                    .andExpect(jsonPath("msg").value("Query successful"))
                    .andExpect(jsonPath("body.total").value(10));
        }
        for (String header : List.of("zh-CN", "zh", "fr-FR", "invalid_%%%", "*", "en;q=0")) {
            mvc.perform(
                            postJson("/IBookBiz/queryBook/page", "{}")
                                    .header("Accept-Language", header))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("msg").value("查询成功"));
        }
        mvc.perform(postJson("/IBookBiz/queryBook/page", "{}"))
                .andExpect(jsonPath("msg").value("查询成功"));
        mvc.perform(postJson("/IBookBiz/queryBook", "{\"id\":1}").header("Accept-Language", "en"))
                .andExpect(jsonPath("msg").value("Operation successful"));
    }

    @Test
    @DisplayName("校验和业务错误按请求语言返回")
    void validationAndBusinessErrorsUseRequestedLanguage() throws Exception {
        for (String language : List.of("zh-CN", "en")) {
            boolean english = language.equals("en");
            mvc.perform(
                            postJson("/IBookBiz/queryBook/page", "{\"pageSize\":0}")
                                    .header("Accept-Language", language))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("code").value("VALIDATION_ERROR"))
                    .andExpect(
                            jsonPath("msg")
                                    .value(
                                            english
                                                    ? "Page size must be between 1 and 100."
                                                    : "每页数量必须在 1–100 之间。"));
            mvc.perform(
                            postJson("/IBookBiz/queryBook", "{\"id\":99999}")
                                    .header("Accept-Language", language))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("code").value("RESOURCE_NOT_FOUND"))
                    .andExpect(jsonPath("msg").value(english ? "Resource not found." : "未找到该资源。"));
            mvc.perform(
                            postJson(
                                            "/IAuthBiz/login",
                                            "{\"username\":\"unknown\",\"password\":\"password123\"}")
                                    .header("Accept-Language", language))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("code").value("UNAUTHORIZED"))
                    .andExpect(
                            jsonPath("msg")
                                    .value(
                                            english
                                                    ? "Invalid username or password."
                                                    : "用户名或密码不正确。"));
        }
        user("i18n_user");
        mvc.perform(
                        postJson(
                                        "/IAuthBiz/register",
                                        "{\"username\":\"i18n_user\",\"password\":\"password123\"}")
                                .header("Accept-Language", "en"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("code").value("USERNAME_EXISTS"))
                .andExpect(
                        jsonPath("msg").value("Username already exists. Please choose another."));
    }

    @Test
    @DisplayName("安全异常在进入控制器前完成翻译")
    void securityErrorsAreLocalizedBeforeControllerDispatch() throws Exception {
        for (String language : List.of("zh-CN", "en")) {
            boolean english = language.equals("en");
            mvc.perform(
                            postJson("/ILoanBiz/queryCurrentLoan/page", "{}")
                                    .header("Accept-Language", language))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("code").value("UNAUTHORIZED"))
                    .andExpect(
                            jsonPath("msg")
                                    .value(english ? "Please sign in to continue." : "请先登录后再操作。"));
            mvc.perform(
                            postJson("/ILoanBiz/queryCurrentLoan/page", "{}")
                                    .header("Accept-Language", language)
                                    .header("Authorization", "Bearer broken"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("code").value("UNAUTHORIZED"))
                    .andExpect(
                            jsonPath("msg")
                                    .value(
                                            english
                                                    ? "Invalid or expired access token. Please sign in again."
                                                    : "登录已过期或令牌无效，请重新登录。"));
        }
    }

    @Test
    @DisplayName("并发请求之间语言不泄漏")
    void concurrentRequestsDoNotLeakLanguage() throws Exception {
        try (ExecutorService pool = Executors.newFixedThreadPool(4)) {
            List<Future<String>> results = new ArrayList<>();
            for (int index = 0; index < 20; index++) {
                String language = index % 2 == 0 ? "en" : "zh-CN";
                results.add(
                        pool.submit(
                                () -> {
                                    MvcResult result =
                                            mvc.perform(
                                                            postJson(
                                                                            "/IBookBiz/queryBook",
                                                                            "{\"id\":99999}")
                                                                    .header(
                                                                            "Accept-Language",
                                                                            language))
                                                    .andExpect(status().isNotFound())
                                                    .andReturn();
                                    return json.readTree(
                                                    result.getResponse()
                                                            .getContentAsString(
                                                                    java.nio.charset
                                                                            .StandardCharsets
                                                                            .UTF_8))
                                            .get("msg")
                                            .asText();
                                }));
            }
            for (int index = 0; index < results.size(); index++) {
                assertThat(results.get(index).get(10, TimeUnit.SECONDS))
                        .isEqualTo(index % 2 == 0 ? "Resource not found." : "未找到该资源。");
            }
        }
        mvc.perform(postJson("/IBookBiz/queryBook", "{\"id\":99999}"))
                .andExpect(jsonPath("msg").value("未找到该资源。"));
    }

    @Test
    @DisplayName("协议与服务异常返回安全的本地化消息")
    void protocolAndServerFailuresUseLocalizedSafeMessages() throws Exception {
        long uid = user("failure_user");
        String bearer = "Bearer " + token(uid, NOW, NOW.plusSeconds(7200), "library-api");
        for (String language : List.of("en", "zh-CN")) {
            boolean english = language.equals("en");
            mvc.perform(get("/IAuthBiz/login").header("Accept-Language", language))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(
                            jsonPath("msg")
                                    .value(english ? "HTTP method not allowed." : "不支持此请求方法。"));
            mvc.perform(
                            post("/IBookBiz/queryBook")
                                    .contentType("text/plain")
                                    .content("invalid")
                                    .header("Accept-Language", language))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(
                            jsonPath("msg")
                                    .value(english ? "Unsupported content type." : "不支持此请求内容类型。"));
            mvc.perform(
                            postJson("/test/errors/forbidden", "{}")
                                    .header("Authorization", bearer)
                                    .header("Accept-Language", language))
                    .andExpect(status().isForbidden())
                    .andExpect(
                            jsonPath("msg")
                                    .value(
                                            english
                                                    ? "You do not have permission to perform this action."
                                                    : "您没有权限执行此操作。"));
            mvc.perform(
                            postJson("/test/errors/unavailable", "{}")
                                    .header("Authorization", bearer)
                                    .header("Accept-Language", language))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(
                            jsonPath("msg")
                                    .value(
                                            english
                                                    ? "Temporarily unavailable. "
                                                            + "Check your current loans before retrying."
                                                    : "服务暂时不可用，请先查询当前借阅，再决定是否重试。"));
            mvc.perform(
                            postJson("/test/errors/unexpected", "{}")
                                    .header("Authorization", bearer)
                                    .header("Accept-Language", language))
                    .andExpect(status().isInternalServerError())
                    .andExpect(
                            jsonPath("msg")
                                    .value(
                                            english
                                                    ? "An unexpected error occurred. Please try again later."
                                                    : "操作暂时未完成，请稍后重试。"))
                    .andExpect(content().string(not(containsString("internal-database-detail"))));
        }
    }

    @Test
    @DisplayName("后端字段校验独立返回中英文消息")
    void backendValidationMessagesAreTranslatedWithoutFrontend() throws Exception {
        for (String language : List.of("zh-CN", "en")) {
            boolean english = language.equals("en");
            mvc.perform(
                            postJson(
                                            "/IAuthBiz/register",
                                            "{\"username\":\"valid_user\",\"password\":\"short\"}")
                                    .header("Accept-Language", language))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("code").value("VALIDATION_ERROR"))
                    .andExpect(
                            jsonPath("msg")
                                    .value(
                                            english
                                                    ? "Password must be 8–72 characters long."
                                                    : "密码长度必须为 8–72 个字符。"));
            mvc.perform(
                            postJson(
                                            "/IAuthBiz/register",
                                            json.writeValueAsString(
                                                    Map.of(
                                                            "username",
                                                            "valid_user",
                                                            "password",
                                                            "密".repeat(25))))
                                    .header("Accept-Language", language))
                    .andExpect(status().isBadRequest())
                    .andExpect(
                            jsonPath("msg")
                                    .value(
                                            english
                                                    ? "Password must be at most 72 UTF-8 bytes."
                                                    : "密码最多为 72 个 UTF-8 字节。"));
            mvc.perform(postJson("/IBookBiz/queryBook", "{}").header("Accept-Language", language))
                    .andExpect(status().isBadRequest())
                    .andExpect(
                            jsonPath("msg")
                                    .value(english ? "Record ID is required." : "记录 ID 不能为空。"));
        }
    }
}
