package com.gccoffee.module.user.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {

    private String nickname;
    private String avatar;
    private String phone;
    private String defaultAddress;
}
