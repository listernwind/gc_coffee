package com.gccoffee.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 储值/余额流水（台账数据源之一）
 */
@Data
@TableName("t_balance_record")
public class BalanceRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private BigDecimal changeAmount;
    private BigDecimal balanceAfter;

    /** RECHARGE 充值 / CONSUME 消费 / REFUND 退款 / PACKAGE 购买套餐 */
    private String bizType;
    private String bizNo;
    private String remark;
    private LocalDateTime createdAt;
}
