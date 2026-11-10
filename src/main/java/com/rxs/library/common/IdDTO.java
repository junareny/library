package com.rxs.library.common;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.rxs.library.exception.LibraryErrorCode;

/** 单记录请求参数。 */
public record IdDTO(
        @NotNull(message = "{" + LibraryErrorCode.Keys.ID_REQUIRED + "}")
                @Positive(message = "{" + LibraryErrorCode.Keys.ID_POSITIVE + "}")
                Long id) {}
