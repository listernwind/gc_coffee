package com.gccoffee.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分流水
 */
@Data
@TableName("t_points_record")
public class PointsRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Integer changePoints;
    private Integer pointsAfter;

    /** EARN 获得 / REDEEM 兑换 / ADJUST 调整 */
    private String bizType;
    private String bizNo;
    private String remark;
    private LocalDateTime createdAt;
}
