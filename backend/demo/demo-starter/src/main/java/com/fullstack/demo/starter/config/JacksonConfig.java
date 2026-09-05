package com.fullstack.demo.starter.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Jackson 序列化配置（参照 fjgtkj-2026-core-json 的 JacksonConfig 思路）。
 * <p>
 * 主键为雪花 ID（Long），超出 JS Number 安全整数范围（±2^53-1）时序列化为字符串，
 * 避免前端精度丢失。
 */
@Configuration
public class JacksonConfig {

    /**
     * JS Number 的最大安全整数
     */
    private static final long JS_MAX_SAFE_INTEGER = 9007199254740991L;

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsSafeLongSerializer() {
        return builder -> builder.serializerByType(Long.class, new JsonSerializer<Long>() {
            @Override
            public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value > JS_MAX_SAFE_INTEGER || value < -JS_MAX_SAFE_INTEGER) {
                    gen.writeString(String.valueOf(value));
                } else {
                    gen.writeNumber(value);
                }
            }
        });
    }
}
