package com.gccoffee.module.user.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RechargeRequest {

    /** 充值金额（元） */
    private BigDecimal amount;
}
