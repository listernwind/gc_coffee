package com.gccoffee.module.user.controller;

import com.gccoffee.common.Result;
import com.gccoffee.module.user.dto.AdminLoginRequest;
import com.gccoffee.module.user.dto.LoginRequest;
import com.gccoffee.module.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /** 微信登录（本地模拟模式下 code 直接作为 openid） */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(userService.loginByWx(req.getCode(), req.getNickname(), req.getAvatar()));
    }

    /** 店长登录（管理后台） */
    @PostMapping("/admin-login")
    public Result<Map<String, Object>> adminLogin(@Valid @RequestBody AdminLoginRequest req) {
        return Result.ok(userService.adminLogin(req.getUsername(), req.getPassword()));
    }
}
