package com.fullstack.common.mybatisplus.entity;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fullstack.common.base.enums.ResultCodeEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 分页查询响应对象。
 */
@Data
@NoArgsConstructor
public class PageQueryResp<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 总记录数。
     */
    private Long total;

    /**
     * 当前页数据列表。
     */
    private List<T> rows;

    /**
     * 当前页码。
     */
    private Long pageNum;

    /**
     * 每页大小。
     */
    private Long pageSize;

    /**
     * 响应状态码。
     */
    private int code;

    /**
     * 响应消息。
     */
    private String msg;

    /**
     * 根据分页对象构建响应，并将记录列表复制为目标类型。
     *
     * @param page  原始分页对象
     * @param clazz 目标记录类型
     * @param <DO>  原始记录类型
     * @param <R>   目标记录类型
     * @return 分页响应对象
     */
    public static <DO, R> PageQueryResp<R> build(IPage<DO> page, Class<R> clazz) {
        List<R> voPage = BeanUtil.copyToList(page.getRecords(), clazz);
        PageQueryResp<R> rspData = new PageQueryResp<>();
        rspData.setCode(ResultCodeEnum.SUCCESS.getCode());
        rspData.setMsg("查询成功");
        rspData.setRows(voPage);
        rspData.setTotal(page.getTotal());
        rspData.setPageNum(page.getCurrent());
        rspData.setPageSize(page.getSize());
        return rspData;
    }

    /**
     * 根据已转换的当前页数据列表构建响应。
     *
     * @param page 原始分页对象
     * @param list 已转换的当前页数据列表
     * @param <DO> 原始记录类型
     * @param <R>  目标记录类型
     * @return 分页响应对象
     */
    public static <DO, R> PageQueryResp<R> build(IPage<DO> page, List<R> list) {
        PageQueryResp<R> rspData = new PageQueryResp<>();
        rspData.setCode(ResultCodeEnum.SUCCESS.getCode());
        rspData.setMsg("查询成功");
        rspData.setRows(list);
        rspData.setTotal(page.getTotal());
        rspData.setPageNum(page.getCurrent());
        rspData.setPageSize(page.getSize());
        return rspData;
    }
}
