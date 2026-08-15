package com.gccoffee.module.drink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_drink_product")
public class DrinkProduct {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;
    private String name;
    private String descText;
    private String image;
    private BigDecimal price;
    /** 标签：热/冰/大杯等，逗号分隔 */
    private String tags;
    /** 库存，-1 表示不限 */
    private Integer stock;
    private Integer active;
    private Integer sort;
    private LocalDateTime createdAt;
}
