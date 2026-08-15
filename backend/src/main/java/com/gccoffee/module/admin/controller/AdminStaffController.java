package com.gccoffee.module.admin.controller;

import com.gccoffee.common.Result;
import com.gccoffee.module.user.entity.User;
import com.gccoffee.module.user.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 派送员账号管理（店长端）
 */
@RestController
@RequestMapping("/api/admin/staff")
@RequiredArgsConstructor
public class AdminStaffController {

    private final UserService userService;

    @GetMapping
    public Result<List<User>> list() {
        return Result.ok(userService.staffList());
    }

    /** 新增（id 为空）/编辑（id 非空，密码留空表示不修改） */
    @PostMapping
    public Result<Void> save(@RequestBody StaffSaveReq req) {
        userService.saveStaff(req.getId(), req.getUsername(), req.getPassword(),
                req.getNickname(), req.getPhone(), req.getStatus());
        return Result.ok();
    }

    /** 启用/禁用 */
    @PostMapping("/{id}/status")
    public Result<Void> status(@PathVariable Long id, @RequestBody StaffSaveReq req) {
        userService.adminSetStatus(id, req.getStatus());
        return Result.ok();
    }

    @Data
    public static class StaffSaveReq {
        private Long id;
        private String username;
        private String password;
        private String nickname;
        private String phone;
        private Integer status;
    }
}
