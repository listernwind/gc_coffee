package com.gccoffee.module.drink.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DrinkOrderRequest {

    @NotEmpty(message = "订单不能为空")
    private List<Item> items;

    /** BALANCE / WX_MOCK */
    private String payType = "WX_MOCK";

    private Long couponId;
    private String remark;

    @Data
    public static class Item {
        @NotNull(message = "商品不能为空")
        private Long productId;
        @NotNull(message = "数量不能为空")
        private Integer quantity;
    }
}
