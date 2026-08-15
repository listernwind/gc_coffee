package com.gccoffee.module.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_setting")
public class Setting {

    @TableId(type = IdType.INPUT)
    private String keyName;

    private String valueText;
    private LocalDateTime updatedAt;
}
