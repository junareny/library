package com.rxs.library.i18n;

import java.util.List;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import com.rxs.library.common.MessageCode;
import com.rxs.library.exception.LibraryErrorCode;

import lombok.RequiredArgsConstructor;

/** 统一解析请求语言并读取消息，未支持或非法语言回退为简体中文。 */
@Component
@RequiredArgsConstructor
public class LocalizedMessages {
    private final MessageSource messageSource;

    public String text(MessageCode code) {
        return messageSource.getMessage(
                code.getMessage(),
                null,
                messageSource.getMessage(
                        LibraryErrorCode.INTERNAL_ERROR.getMessage(),
                        null,
                        LocaleContextHolder.getLocale()),
                LocaleContextHolder.getLocale());
    }

    public static Locale resolve(HttpServletRequest request) {
        String header = request.getHeader("Accept-Language");
        if (header != null && !header.isBlank()) {
            try {
                Locale locale =
                        Locale.lookup(
                                Locale.LanguageRange.parse(header),
                                List.of(Locale.SIMPLIFIED_CHINESE, Locale.CHINESE, Locale.ENGLISH));
                if (locale != null) {
                    return locale.getLanguage().equals("en")
                            ? Locale.ENGLISH
                            : Locale.SIMPLIFIED_CHINESE;
                }
            } catch (IllegalArgumentException ignored) {
                // 非法请求头回退默认语言，避免语言协商本身导致报错。
            }
        }
        return Locale.SIMPLIFIED_CHINESE;
    }
}
