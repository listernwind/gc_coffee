package com.gccoffee.module.coffee.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PackageBuyRequest {

    @NotNull(message = "请选择套餐")
    private Long packageId;

    /** BALANCE / WX_MOCK */
    private String payType = "WX_MOCK";
}
