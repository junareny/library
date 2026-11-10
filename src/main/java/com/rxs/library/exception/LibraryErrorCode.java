package com.rxs.library.exception;

import org.springframework.http.HttpStatus;

import com.rxs.library.common.MessageCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 参考 PdmErrorCode，将对外错误码、国际化资源键和 HTTP 状态集中定义。 */
@Getter
@AllArgsConstructor
public enum LibraryErrorCode implements MessageCode {
    /** 用户名已存在。 */
    USERNAME_EXISTS("USERNAME_EXISTS", "library.user.username.exists", HttpStatus.CONFLICT),
    /** 图书没有可借库存。 */
    OUT_OF_STOCK("OUT_OF_STOCK", "library.book.out.of.stock", HttpStatus.CONFLICT),
    /** 用户已存在该书的未归还借阅。 */
    ALREADY_BORROWED("ALREADY_BORROWED", "library.loan.already.borrowed", HttpStatus.CONFLICT),
    /** 当前借阅数量已达上限。 */
    LOAN_LIMIT_EXCEEDED("LOAN_LIMIT_EXCEEDED", "library.loan.limit.exceeded", HttpStatus.CONFLICT),
    /** 借阅记录已归还。 */
    ALREADY_RETURNED("ALREADY_RETURNED", "library.loan.already.returned", HttpStatus.CONFLICT),
    /** 资源不存在或不属于当前用户。 */
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "library.resource.not.found", HttpStatus.NOT_FOUND),
    /** 请求格式或参数无效。 */
    VALIDATION_ERROR("VALIDATION_ERROR", "library.request.invalid", HttpStatus.BAD_REQUEST),
    /** 登录账号或密码不正确。 */
    UNAUTHORIZED("UNAUTHORIZED", "library.auth.credentials.invalid", HttpStatus.UNAUTHORIZED),
    /** 当前请求需要先登录。 */
    AUTHENTICATION_REQUIRED("UNAUTHORIZED", "library.auth.required", HttpStatus.UNAUTHORIZED),
    /** 访问令牌无效或已过期。 */
    INVALID_TOKEN("UNAUTHORIZED", "library.auth.token.invalid", HttpStatus.UNAUTHORIZED),
    /** 令牌对应用户已不存在。 */
    USER_NO_LONGER_EXISTS("UNAUTHORIZED", "library.auth.user.not.exists", HttpStatus.UNAUTHORIZED),
    /** 当前用户没有操作权限。 */
    FORBIDDEN("FORBIDDEN", "library.access.forbidden", HttpStatus.FORBIDDEN),
    /** 接口不支持请求使用的方法。 */
    METHOD_NOT_ALLOWED(
            "METHOD_NOT_ALLOWED",
            "library.request.method.not.allowed",
            HttpStatus.METHOD_NOT_ALLOWED),
    /** 接口不支持请求的内容类型。 */
    UNSUPPORTED_MEDIA_TYPE(
            "UNSUPPORTED_MEDIA_TYPE",
            "library.request.media.unsupported",
            HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    /** 数据库等依赖暂时不可用。 */
    SERVICE_UNAVAILABLE(
            "SERVICE_UNAVAILABLE", "library.service.unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    /** 未预期异常的安全兜底提示。 */
    INTERNAL_ERROR("INTERNAL_ERROR", "library.internal.error", HttpStatus.INTERNAL_SERVER_ERROR),
    /** 用户名不能为空。 */
    USERNAME_REQUIRED("VALIDATION_ERROR", Keys.USERNAME_REQUIRED, HttpStatus.BAD_REQUEST),
    /** 用户名长度或空白字符不符合要求。 */
    USERNAME_FORMAT("VALIDATION_ERROR", Keys.USERNAME_FORMAT, HttpStatus.BAD_REQUEST),
    /** 密码不能为空。 */
    PASSWORD_REQUIRED("VALIDATION_ERROR", Keys.PASSWORD_REQUIRED, HttpStatus.BAD_REQUEST),
    /** 密码字符长度不符合要求。 */
    PASSWORD_LENGTH("VALIDATION_ERROR", Keys.PASSWORD_LENGTH, HttpStatus.BAD_REQUEST),
    /** 密码超出 BCrypt 支持的字节上限。 */
    PASSWORD_BYTES("VALIDATION_ERROR", Keys.PASSWORD_BYTES, HttpStatus.BAD_REQUEST),
    /** 记录主键不能为空。 */
    ID_REQUIRED("VALIDATION_ERROR", Keys.ID_REQUIRED, HttpStatus.BAD_REQUEST),
    /** 记录主键必须为正数。 */
    ID_POSITIVE("VALIDATION_ERROR", Keys.ID_POSITIVE, HttpStatus.BAD_REQUEST),
    /** 书名查询条件长度超出限制。 */
    TITLE_LENGTH("VALIDATION_ERROR", Keys.TITLE_LENGTH, HttpStatus.BAD_REQUEST),
    /** 页码不能小于一。 */
    PAGE_CURRENT("VALIDATION_ERROR", Keys.PAGE_CURRENT, HttpStatus.BAD_REQUEST),
    /** 每页数量必须在允许范围内。 */
    PAGE_SIZE("VALIDATION_ERROR", Keys.PAGE_SIZE, HttpStatus.BAD_REQUEST);

    /** 对外业务码，不随语言变化。 */
    private final String code;

    /** 中英文资源文件共同使用的消息键。 */
    private final String message;

    /** 该错误对应的 HTTP 状态。 */
    private final HttpStatus status;

    /** 注解参数要求编译期常量，这些资源键同时由对应枚举项引用。 */
    public static final class Keys {
        public static final String USERNAME_REQUIRED = "validation.username.required";
        public static final String USERNAME_FORMAT = "validation.username.format";
        public static final String PASSWORD_REQUIRED = "validation.password.required";
        public static final String PASSWORD_LENGTH = "validation.password.length";
        public static final String PASSWORD_BYTES = "validation.password.bytes";
        public static final String ID_REQUIRED = "validation.id.required";
        public static final String ID_POSITIVE = "validation.id.positive";
        public static final String TITLE_LENGTH = "validation.title.length";
        public static final String PAGE_CURRENT = "validation.page.current";
        public static final String PAGE_SIZE = "validation.page.size";

        private Keys() {}
    }
}
