package com.gccoffee.module.drink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 饮品订单（到店自取/现场制作）
 */
@Data
@TableName("t_drink_order")
public class DrinkOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private Long userId;

    /** PAID 已支付 / MAKING 制作中 / READY 待取餐 / FINISHED 已完成 / CANCELLED 已取消 */
    private String status;

    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    /** BALANCE / WX_MOCK */
    private String payType;
    private Long couponId;
    /** 取餐码（订单号后4位） */
    private String pickupCode;
    private String remark;
    private LocalDateTime paidAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
}
