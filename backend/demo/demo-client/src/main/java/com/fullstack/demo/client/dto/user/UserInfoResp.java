package com.fullstack.demo.client.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 当前用户信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private String nickname;

    /**
     * 注册时间
     */
    private LocalDateTime createTime;
}
