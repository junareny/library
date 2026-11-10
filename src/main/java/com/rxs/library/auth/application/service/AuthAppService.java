package com.rxs.library.auth.application.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rxs.library.auth.domain.bo.CredentialsBO;
import com.rxs.library.auth.domain.vo.TokenVO;
import com.rxs.library.auth.domain.vo.UserVO;
import com.rxs.library.common.ApiException;
import com.rxs.library.exception.LibraryErrorCode;
import com.rxs.library.user.domain.entity.UserTPO;
import com.rxs.library.user.domain.service.IUserTableService;

/** 注册、密码验证与访问令牌签发服务。 */
@Service
public class AuthAppService {
    private final IUserTableService users;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final Clock clock;
    private final String issuer;
    private final long ttl;
    private final String dummyHash;

    public AuthAppService(
            IUserTableService users,
            PasswordEncoder passwords,
            JwtEncoder encoder,
            Clock clock,
            @Value("${app.jwt-issuer}") String issuer,
            @Value("${app.jwt-ttl-seconds}") long ttl) {
        this.users = users;
        this.passwords = passwords;
        this.encoder = encoder;
        this.clock = clock;
        this.issuer = issuer;
        this.ttl = ttl;
        this.dummyHash = passwords.encode("dummy-password-for-timing");
    }

    /**
     * 创建用户，仅保存 BCrypt 哈希；唯一用户名冲突由事务回滚并转换为业务异常。
     *
     * @param credentials 已校验的注册信息
     * @return 新用户的公开身份
     */
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(CredentialsBO credentials) {
        try {
            UserTPO user =
                    new UserTPO(
                            credentials.username(),
                            passwords.encode(credentials.password()),
                            clock.instant());
            users.save(user);
            return new UserVO(user.getId(), user.getUsername());
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(LibraryErrorCode.USERNAME_EXISTS);
        }
    }

    /**
     * 验证密码并签发访问令牌；用户名不存在时仍执行密码哈希比较。
     *
     * @param credentials 已校验的登录信息
     * @return Bearer 访问令牌及有效秒数
     */
    @Transactional(readOnly = true)
    public TokenVO login(CredentialsBO credentials) {
        UserTPO user = users.findByUsername(credentials.username());
        boolean valid =
                passwords.matches(
                        credentials.password(), user == null ? dummyHash : user.getPasswordHash());
        if (user == null || !valid) {
            throw new ApiException(LibraryErrorCode.UNAUTHORIZED);
        }
        Instant now = clock.instant();
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(issuer)
                        .subject(user.getId().toString())
                        .issuedAt(now)
                        .expiresAt(now.plusSeconds(ttl))
                        .build();
        String token =
                encoder.encode(
                                JwtEncoderParameters.from(
                                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                        .getTokenValue();
        return new TokenVO(token, "Bearer", ttl);
    }
}
