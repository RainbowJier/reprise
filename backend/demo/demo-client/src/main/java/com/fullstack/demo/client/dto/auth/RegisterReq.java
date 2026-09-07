package com.fullstack.demo.client.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 注册请求
 */
@Data
public class RegisterReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 登录名：4-32 位字母数字下划线
     */
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_]{4,32}$", message = "用户名须为 4-32 位字母数字下划线")
    private String username;

    /**
     * 密码：6-64 位
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度须为 6-64 位")
    private String password;

    /**
     * 昵称（可选，缺省同用户名）
     */
    @Size(max = 50, message = "昵称最长 50 字符")
    private String nickname;
}
