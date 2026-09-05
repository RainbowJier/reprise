package com.fullstack.common.base.util;

import com.fullstack.common.base.entity.AjaxResult;
import com.fullstack.common.base.exception.BusinessException;

/**
 * Feign 远程调用结果解析工具类
 * <p>
 * 当前 demo 为单服务暂无 Feign 调用，该工具与 fjgtkj-2026 core-base 保持一致，
 * 供未来跨服务调用（application -> domain Gateway -> infrastructure -> Feign）使用。
 */
public class FeignResultUtil {

    /**
     * 解析 Feign 调用结果，失败或数据为空时抛 BusinessException（触发事务回滚）
     */
    public static <T> T getDataOrThrow(AjaxResult<T> result, String errorMsg) {
        if (!result.isSuccess() || result.getData() == null) {
            throw new BusinessException(errorMsg + ": " + result.getMsg());
        }
        return result.getData();
    }

    /**
     * 解析 Feign 调用结果，失败或数据为空时返回默认值
     */
    public static <T> T getDataOrElse(AjaxResult<T> result, T defaultValue) {
        if (!result.isSuccess() || result.getData() == null) {
            return defaultValue;
        }
        return result.getData();
    }

}
