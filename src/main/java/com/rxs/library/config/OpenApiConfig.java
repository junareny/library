package com.rxs.library.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/** 接口文档及 Bearer JWT 认证说明。 */
@Configuration
@OpenAPIDefinition(
        info =
                @Info(
                        title = "Library Lending API",
                        version = "1.0.0",
                        description = "Book catalog and lending backend for MySQL 5.7"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {}
