package com.gccoffee.module.coffee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户月度套餐（额度账户）
 */
@Data
@TableName("t_user_package")
public class UserPackage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long packageId;
    private String packageName;
    /** 所属月份，如 2025-08 */
    private String month;
    private Integer totalQuota;
    private Integer usedQuota;
    private BigDecimal amount;
    private LocalDateTime createdAt;
}
