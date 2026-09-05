package com.fullstack.common.mybatisplus.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础实体类
 */
@Data
public abstract class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT, jdbcType = JdbcType.TIMESTAMP)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE, jdbcType = JdbcType.TIMESTAMP)
    private LocalDateTime updateTime;

    /**
     * 创建人ID（demo 未接入认证，暂不填充）
     */
    @TableField(fill = FieldFill.INSERT, jdbcType = JdbcType.BIGINT)
    private Long createBy;

    /**
     * 更新人ID（demo 未接入认证，暂不填充）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE, jdbcType = JdbcType.BIGINT)
    private Long updateBy;

    /**
     * 逻辑删除标识（0-正常，1-删除），根据需求选择是否逻辑删除
     */
    @TableLogic(value = "0", delval = "1")
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
