package com.gccoffee.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    /** wx.login 得到的 code；本地模拟模式下可为任意字符串 */
    @NotBlank(message = "code 不能为空")
    private String code;

    private String nickname;
    private String avatar;
}
