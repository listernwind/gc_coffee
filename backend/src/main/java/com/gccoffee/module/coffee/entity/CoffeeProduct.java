package com.gccoffee.module.coffee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 咖啡液单品（按瓶/按包销售）
 */
@Data
@TableName("t_coffee_product")
public class CoffeeProduct {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    /** 规格，如 500ml/袋 */
    private String spec;
    private String descText;
    private String image;
    private BigDecimal price;
    private Integer active;
    private Integer sort;
    private LocalDateTime createdAt;
}
