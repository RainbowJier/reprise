package com.fullstack.demo.domain.user;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fullstack.common.mybatisplus.entity.BaseIdEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 用户实体（场景 01：登录与认证）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("users")
public class User extends BaseIdEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 登录名（唯一索引）
     */
    private String username;

    /**
     * BCrypt 密码哈希
     */
    private String password;

    /**
     * 昵称（缺省同 username）
     */
    private String nickname;
}
