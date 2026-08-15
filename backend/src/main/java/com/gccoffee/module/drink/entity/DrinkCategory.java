package com.gccoffee.module.drink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_drink_category")
public class DrinkCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private Integer sort;
    private LocalDateTime createdAt;
}
