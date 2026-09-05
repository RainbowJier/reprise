package com.fullstack.common.base.entity;

import cn.hutool.core.bean.BeanUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;

/**
 * 表格分页数据对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class PageResult implements Serializable {
    private long total;

    private long pageNum;

    private long pageSize;

    private List<?> rows;

    public PageResult(List<?> list, long total) {
        this.rows = list;
        this.total = Math.toIntExact(total);
    }

    public static <T> PageResult ok(long total, long pageNum, long pageSize, List<T> rows) {
        return new PageResult(
                total,
                pageNum,
                pageSize,
                rows
        );
    }

    public static <E, V> PageResult ok(long total, long pageNum, long pageSize, List<E> rows, Class<V> voClass) {
        List<V> voList = BeanUtil.copyToList(rows, voClass);

        return new PageResult(
                total,
                pageNum,
                pageSize,
                voList
        );
    }

    /**
     * 支持两步转换的分页方法: Entity -> PO -> Resp
     * 使用 MapStruct Converter 进行链式转换
     *
     * @param total      总条数
     * @param pageNum    当前页码
     * @param pageSize   每页大小
     * @param rows       当前页数据
     * @param entityToPO Entity 转 PO 的函数
     * @param poToResp   PO 转 Resp 的函数
     * @param <E>        Entity 类型
     * @param <P>        PO 类型
     * @param <R>        Resp 类型
     * @return 分页结果
     */
    public static <E, P, R> PageResult ok(
            long total,
            long pageNum,
            long pageSize,
            List<E> rows,
            Function<List<E>, List<P>> entityToPO,
            Function<List<P>, List<R>> poToResp
    ) {
        List<P> poList = entityToPO.apply(rows);
        List<R> respList = poToResp.apply(poList);

        return new PageResult(
                total,
                pageNum,
                pageSize,
                respList
        );
    }

}
