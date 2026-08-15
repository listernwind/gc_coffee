package com.gccoffee.module.coffee.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReserveRequest {

    @NotNull(message = "请选择咖啡液产品")
    private Long productId;

    @NotNull(message = "请选择数量")
    @Min(value = 1, message = "数量至少为 1")
    private Integer quantity;

    /** 是否优先使用本月套餐额度（默认 true） */
    private Boolean usePackage = true;

    /** BALANCE / WX_MOCK，超出额度的部分如何支付 */
    private String payType = "WX_MOCK";

    /** 派送日期（默认次日） */
    private LocalDate deliveryDate;

    @NotBlank(message = "请选择派送时段")
    private String timeSlot;

    @NotBlank(message = "请填写楼栋门牌号")
    private String address;

    @NotBlank(message = "请填写联系人")
    private String contactName;

    @NotBlank(message = "请填写联系电话")
    private String contactPhone;

    private String remark;
}
