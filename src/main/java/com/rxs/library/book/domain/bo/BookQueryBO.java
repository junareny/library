package com.rxs.library.book.domain.bo;

import jakarta.validation.constraints.Size;

import com.rxs.library.exception.LibraryErrorCode;

/** 按书名进行字面包含查询。 */
public record BookQueryBO(
        @Size(max = 255, message = "{" + LibraryErrorCode.Keys.TITLE_LENGTH + "}") String title) {}
