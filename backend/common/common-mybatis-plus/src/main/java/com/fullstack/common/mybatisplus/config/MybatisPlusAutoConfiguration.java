package com.fullstack.common.mybatisplus.config;

import com.fullstack.common.mybatisplus.handler.MyMetaObjectHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * MyBatis-Plus 公共能力自动配置。
 */
@AutoConfiguration
@Import({MybatisPlusConfig.class, MyMetaObjectHandler.class})
public class MybatisPlusAutoConfiguration {
}
