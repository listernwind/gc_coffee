package com.gccoffee.module.coffee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 咖啡液预订单（次日派送）
 */
@Data
@TableName("t_coffee_reservation")
public class CoffeeReservation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private Long userId;
    private Long productId;
    private String productName;
    private Integer quantity;

    /** 套餐额度抵扣瓶数 */
    private Integer packageUsed;

    /** 需支付瓶数 */
    private Integer payQuantity;

    private BigDecimal unitPrice;

    /** 实付金额（= payQuantity * unitPrice） */
    private BigDecimal amount;

    /** BALANCE / WX_MOCK */
    private String payType;

    /** PENDING 待确认 / CONFIRMED 已确认 / DELIVERING 派送中 / DELIVERED 已送达 / CANCELLED 已取消 */
    private String status;

    private LocalDate deliveryDate;
    private String timeSlot;
    /** 小区 + 楼栋门牌号 */
    private String address;
    private String contactName;
    private String contactPhone;
    private String remark;
    private LocalDateTime cancelAt;
    private LocalDateTime createdAt;
}
