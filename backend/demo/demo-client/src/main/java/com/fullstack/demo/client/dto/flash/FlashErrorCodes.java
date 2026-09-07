package com.fullstack.demo.client.dto.flash;

/**
 * 秒杀域业务错误码（61xx 分段）。
 * <p>
 * 与通用 HTTP 语义码（401/404/409…）区分开：秒杀的「失败」大多是正常业务结果
 * （手慢了、重复抢），不是客户端错误。前端按 code !== 200 统一拒绝并直接展示 msg。
 */
public final class FlashErrorCodes {

    /** 活动未开始 */
    public static final String NOT_STARTED = "6101";

    /** 活动已结束 */
    public static final String ENDED = "6102";

    /** 已售罄 */
    public static final String SOLD_OUT = "6103";

    /** 重复抢购（每人限购一件） */
    public static final String DUPLICATE = "6104";

    private FlashErrorCodes() {
    }
}
