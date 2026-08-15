package com.gccoffee.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    @JsonIgnore
    private String openid;

    /** 店长账号用户名（普通用户为空） */
    private String username;

    @JsonIgnore
    private String password;

    private String nickname;
    private String avatar;
    private String phone;

    /** USER / ADMIN */
    private String role;

    /** 储值余额 */
    private BigDecimal balance;

    /** 积分 */
    private Integer points;

    /** 累计消费（用于等级） */
    private BigDecimal totalSpend;

    /** 默认配送门牌号 */
    private String defaultAddress;

    /** 1正常 0禁用 */
    private Integer status;

    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
