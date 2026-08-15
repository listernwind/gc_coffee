package com.gccoffee.module.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 海报（首页轮播 + 活动页展示）
 */
@Data
@TableName("t_poster")
public class Poster {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String image;
    /** NONE / ACTIVITY */
    private String linkType;
    private Long linkId;
    private Integer active;
    private Integer sort;
    private LocalDateTime createdAt;
}
