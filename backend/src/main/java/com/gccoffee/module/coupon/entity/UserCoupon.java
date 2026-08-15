package com.gccoffee.module.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户优惠券
 */
@Data
@TableName("t_user_coupon")
public class UserCoupon {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long templateId;
    private String name;

    /** FULL_REDUCTION / DISCOUNT / CASH */
    private String type;

    private BigDecimal value;
    private BigDecimal minAmount;

    /** UNUSED / USED / EXPIRED */
    private String status;

    /** CLAIM 领取 / REDEEM 积分兑换 / GIFT 赠送 */
    private String source;

    private LocalDateTime expireAt;
    private LocalDateTime usedAt;
    private String usedOrderNo;
    private LocalDateTime createdAt;
}
