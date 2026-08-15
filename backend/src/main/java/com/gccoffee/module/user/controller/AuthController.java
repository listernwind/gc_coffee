package com.gccoffee.module.user.controller;

import com.gccoffee.common.Result;
import com.gccoffee.module.user.dto.AdminLoginRequest;
import com.gccoffee.module.user.dto.LoginRequest;
import com.gccoffee.module.user.dto.RegisterRequest;
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

    /** 普通用户账号密码登录 */
    @PostMapping("/password-login")
    public Result<Map<String, Object>> passwordLogin(@Valid @RequestBody AdminLoginRequest req) {
        return Result.ok(userService.passwordLogin(req.getUsername(), req.getPassword()));
    }

    /** 普通用户注册（店长/派送员不开放注册） */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        return Result.ok(userService.register(req.getUsername(), req.getPassword(), req.getNickname(), req.getPhone()));
    }

    /** 店长登录（管理后台） */
    @PostMapping("/admin-login")
    public Result<Map<String, Object>> adminLogin(@Valid @RequestBody AdminLoginRequest req) {
        return Result.ok(userService.adminLogin(req.getUsername(), req.getPassword()));
    }

    /** 派送员登录（派送工作台） */
    @PostMapping("/staff-login")
    public Result<Map<String, Object>> staffLogin(@Valid @RequestBody AdminLoginRequest req) {
        return Result.ok(userService.staffLogin(req.getUsername(), req.getPassword()));
    }
}
