package com.gccoffee.module.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gccoffee.common.PageResult;
import com.gccoffee.common.Result;
import com.gccoffee.module.user.entity.User;
import com.gccoffee.module.user.mapper.UserMapper;
import com.gccoffee.module.user.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 会员管理
 */
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final UserMapper userMapper;
    private final UserService userService;

    @GetMapping
    public Result<PageResult<User>> list(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "20") long size,
                                         @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<User>()
                .eq(User::getRole, "USER");
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(User::getNickname, keyword)
                    .or().like(User::getPhone, keyword)
                    .or().like(User::getOpenid, keyword));
        }
        qw.orderByDesc(User::getId);
        return Result.ok(PageResult.of(userMapper.selectPage(new Page<>(page, size), qw)));
    }

    /** 调整余额（负数为扣减） */
    @PostMapping("/{id}/balance")
    public Result<Void> adjustBalance(@PathVariable Long id, @RequestBody AdjustReq req) {
        userService.adminAdjustBalance(id, req.getAmount() == null ? BigDecimal.ZERO : req.getAmount(), req.getRemark());
        return Result.ok();
    }

    /** 调整积分（负数为扣减） */
    @PostMapping("/{id}/points")
    public Result<Void> adjustPoints(@PathVariable Long id, @RequestBody AdjustReq req) {
        userService.adminAdjustPoints(id, req.getPoints() == null ? 0 : req.getPoints(), req.getRemark());
        return Result.ok();
    }

    /** 启用/禁用 */
    @PostMapping("/{id}/status")
    public Result<Void> setStatus(@PathVariable Long id, @RequestBody StatusReq req) {
        userService.adminSetStatus(id, req.getStatus());
        return Result.ok();
    }

    @Data
    public static class AdjustReq {
        private BigDecimal amount;
        private Integer points;
        private String remark;
    }

    @Data
    public static class StatusReq {
        private Integer status;
    }
}
