package com.gccoffee.module.coffee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 咖啡液月度套餐（如 30瓶/月）
 */
@Data
@TableName("t_coffee_package")
public class CoffeePackage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private Integer bottleCount;
    private BigDecimal price;
    private String extraDesc;
    private Integer active;
    private Integer sort;
    private LocalDateTime createdAt;
}
