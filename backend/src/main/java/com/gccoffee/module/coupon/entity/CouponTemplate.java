package com.gccoffee.module.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板（后台创建，用户领取/积分兑换）
 */
@Data
@TableName("t_coupon_template")
public class CouponTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** FULL_REDUCTION 满减 / DISCOUNT 折扣 / CASH 现金券 */
    private String type;

    /** 满减=减免金额，折扣=折扣率(0.9 表示9折)，现金券=面额 */
    private BigDecimal value;

    /** 使用门槛（满X元可用） */
    private BigDecimal minAmount;

    /** 领取后有效天数 */
    private Integer validDays;

    /** 发放总量，0 表示不限 */
    private Integer totalCount;

    private Integer issuedCount;

    /** 每人限领 */
    private Integer perUserLimit;

    /** 积分兑换所需积分，0 表示不可兑换 */
    private Integer redeemPoints;

    private Integer active;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private LocalDateTime createdAt;
}
