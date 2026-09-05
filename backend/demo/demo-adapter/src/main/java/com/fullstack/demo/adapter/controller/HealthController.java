package com.fullstack.demo.adapter.controller;

import cn.hutool.core.date.DateUtil;
import com.fullstack.common.base.entity.AjaxResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Map;

/**
 * 健康检查控制器
 */
@RestController
public class HealthController {

    /**
     * 健康检查。
     *
     * @return 服务状态
     */
    @GetMapping("/health")
    public AjaxResult<Map<String, String>> health() {
        return AjaxResult.success(Map.of(
                "status", "UP",
                "time", DateUtil.formatDateTime(new Date())
        ));
    }
}
