package com.gccoffee.module.coffee.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 月度咖啡液详情：本月花费 / 剩余额度 / 预订明细
 */
@Data
public class MonthlySummaryVO {

    private String month;
    /** 本月花费（套餐购买 + 按次支付） */
    private BigDecimal spend;
    /** 套餐总瓶数 */
    private Integer quotaTotal;
    /** 已用瓶数 */
    private Integer quotaUsed;
    /** 剩余瓶数 */
    private Integer quotaRemain;
    /** 本月的套餐卡 */
    private List<PackageCardVO> packages;
    /** 本月预订明细 */
    private List<com.gccoffee.module.coffee.entity.CoffeeReservation> records;

    @Data
    public static class PackageCardVO {
        private Long id;
        private String packageName;
        private Integer totalQuota;
        private Integer usedQuota;
        private Integer remain;
        private BigDecimal amount;
    }
}
