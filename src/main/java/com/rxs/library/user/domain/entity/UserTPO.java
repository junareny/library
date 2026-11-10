package com.rxs.library.user.domain.entity;

import java.time.Instant;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

/** 用户持久化模型，密码字段仅保存 BCrypt 哈希。 */
@Data
@Accessors(chain = true)
@TableName("users")
public class UserTPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    @JsonIgnore @ToString.Exclude private String passwordHash;

    private Instant createdAt;

    public UserTPO() {}

    public UserTPO(String username, String passwordHash, Instant createdAt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }
}
