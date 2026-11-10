package com.rxs.library.auth.domain.bo;

import java.nio.charset.StandardCharsets;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rxs.library.exception.LibraryErrorCode;

/** 注册及登录请求，限制密码 UTF-8 字节数以避免 BCrypt 截断。 */
public record CredentialsBO(
        @NotBlank(message = "{" + LibraryErrorCode.Keys.USERNAME_REQUIRED + "}")
                @Pattern(
                        regexp = "[^\\s]{3,50}",
                        message = "{" + LibraryErrorCode.Keys.USERNAME_FORMAT + "}")
                String username,
        @NotBlank(message = "{" + LibraryErrorCode.Keys.PASSWORD_REQUIRED + "}")
                @Size(
                        min = 8,
                        max = 72,
                        message = "{" + LibraryErrorCode.Keys.PASSWORD_LENGTH + "}")
                String password) {
    @AssertTrue(message = "{" + LibraryErrorCode.Keys.PASSWORD_BYTES + "}")
    @JsonIgnore
    public boolean isPasswordWithinBcryptLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
