package com.rxs.library.auth.domain.vo;

/** 访问令牌及有效秒数。 */
public record TokenVO(String accessToken, String tokenType, long expiresIn) {}
