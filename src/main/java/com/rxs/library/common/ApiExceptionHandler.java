package com.rxs.library.common;

import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.rxs.library.exception.LibraryErrorCode;
import com.rxs.library.i18n.LocalizedMessages;

import lombok.RequiredArgsConstructor;

/** 统一输出 Result，避免向客户端暴露数据库或堆栈信息。 */
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionHandler {
    private final LocalizedMessages messages;
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Result<Void>> business(ApiException ex) {
        return problem(ex.errorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Result<Void>> validation(MethodArgumentNotValidException ex) {
        String detail =
                ex.getBindingResult().getFieldErrors().stream()
                        .sorted(Comparator.comparing(FieldError::getField))
                        .map(FieldError::getDefaultMessage)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.joining("; "));
        if (detail.isBlank()) {
            detail = messages.text(LibraryErrorCode.VALIDATION_ERROR);
        }
        return ResponseEntity.badRequest()
                .body(Result.failure(LibraryErrorCode.VALIDATION_ERROR, detail));
    }

    @ExceptionHandler({
        ConstraintViolationException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        HandlerMethodValidationException.class
    })
    ResponseEntity<Result<Void>> invalid(Exception ex) {
        return problem(LibraryErrorCode.VALIDATION_ERROR);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    ResponseEntity<Result<Void>> missing(Exception ex) {
        return problem(LibraryErrorCode.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<Result<Void>> method(Exception ex) {
        return problem(LibraryErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<Result<Void>> media(Exception ex) {
        return problem(LibraryErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler({
        TransientDataAccessException.class,
        DataAccessResourceFailureException.class,
        CannotCreateTransactionException.class
    })
    ResponseEntity<Result<Void>> unavailable(Exception ex) {
        LOGGER.warn("Temporary database failure", ex);
        return problem(LibraryErrorCode.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Result<Void>> unexpected(Exception ex) {
        LOGGER.error("Unexpected request failure", ex);
        return problem(LibraryErrorCode.INTERNAL_ERROR);
    }

    private ResponseEntity<Result<Void>> problem(LibraryErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(Result.failure(errorCode, messages.text(errorCode)));
    }
}
