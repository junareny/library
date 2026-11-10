package com.rxs.library;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxs.library.common.ApiException;
import com.rxs.library.common.SuccessCode;
import com.rxs.library.exception.LibraryErrorCode;
import com.rxs.library.i18n.LocalizedMessages;
import com.rxs.library.i18n.LocalizedResultFactory;

/** 无需数据库的消息工厂与错误枚举单元测试。 */
class LocalizedMessageTest {
    @Test
    @DisplayName("结果工厂在序列化前完成消息翻译")
    void backendResultFactoryReturnsLocalizedMessagesBeforeSerialization() {
        var messages = new ResourceBundleMessageSource();
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        var factory = new LocalizedResultFactory(new LocalizedMessages(messages));
        var previous = LocaleContextHolder.getLocaleContext();
        try {
            for (var locale : List.of(Locale.ENGLISH, Locale.SIMPLIFIED_CHINESE)) {
                LocaleContextHolder.setLocale(locale);
                boolean english = locale.getLanguage().equals("en");
                assertThat(factory.success("body").msg())
                        .isEqualTo(english ? "Operation successful" : "操作成功");
                assertThat(factory.page(new Page<>()).msg())
                        .isEqualTo(english ? "Query successful" : "查询成功");
            }
        } finally {
            LocaleContextHolder.setLocaleContext(previous);
        }
    }

    @Test
    @DisplayName("错误枚举资源完整且状态一致")
    void errorEnumsHaveCompleteMessagesAndConsistentStatuses() throws Exception {
        var source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        Set<String> entries = new HashSet<>();
        for (LibraryErrorCode error : LibraryErrorCode.values()) {
            assertThat(entries.add(error.getCode() + ":" + error.getMessage())).isTrue();
            ApiException exception = new ApiException(error);
            assertThat(exception.code()).isEqualTo(error.getCode());
            assertThat(exception.status()).isEqualTo(error.getStatus());
            assertThat(exception.errorCode()).isSameAs(error);
            for (var locale : List.of(Locale.ENGLISH, Locale.SIMPLIFIED_CHINESE)) {
                assertThat(source.getMessage(error.getMessage(), null, locale)).isNotBlank();
            }
        }
        assertThat(LibraryErrorCode.INVALID_TOKEN.getCode()).isEqualTo("UNAUTHORIZED");
        assertThat(LibraryErrorCode.AUTHENTICATION_REQUIRED.getCode()).isEqualTo("UNAUTHORIZED");

        // 直接检查每份资源，防止默认语言回退掩盖缺失的翻译。
        for (String resource :
                List.of(
                        "messages.properties",
                        "messages_zh_CN.properties",
                        "messages_en.properties")) {
            Properties messages = new Properties();
            try (var stream = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertThat(stream).as("消息资源 %s", resource).isNotNull();
                messages.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            }
            for (LibraryErrorCode error : LibraryErrorCode.values()) {
                assertThat(messages.getProperty(error.getMessage()))
                        .as("%s 中的错误消息 %s", resource, error.name())
                        .isNotBlank();
            }
            for (SuccessCode success : SuccessCode.values()) {
                assertThat(messages.getProperty(success.getMessage()))
                        .as("%s 中的成功消息 %s", resource, success.name())
                        .isNotBlank();
            }
        }
    }
}
