package com.gccoffee.module.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 月度/季度活动
 */
@Data
@TableName("t_activity")
public class Activity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    /** MONTHLY 月度 / QUARTERLY 季度 */
    private String type;
    private String subtitle;
    private String content;
    private String image;
    private LocalDate startAt;
    private LocalDate endAt;
    private Integer active;
    private Integer sort;
    private LocalDateTime createdAt;
}
