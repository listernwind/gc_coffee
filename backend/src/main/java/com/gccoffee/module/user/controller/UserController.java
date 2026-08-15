package com.gccoffee.module.user.controller;

import com.gccoffee.common.PageResult;
import com.gccoffee.common.Result;
import com.gccoffee.module.user.dto.ProfileUpdateRequest;
import com.gccoffee.module.user.dto.RechargeRequest;
import com.gccoffee.module.user.dto.UserProfileVO;
import com.gccoffee.module.user.entity.BalanceRecord;
import com.gccoffee.module.user.entity.PointsRecord;
import com.gccoffee.module.user.entity.User;
import com.gccoffee.module.user.service.UserService;
import com.gccoffee.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public Result<UserProfileVO> me() {
        return Result.ok(userService.profile(UserContext.uid()));
    }

    @PutMapping("/me")
    public Result<Void> update(@RequestBody ProfileUpdateRequest req) {
        userService.updateProfile(UserContext.uid(), req.getNickname(), req.getAvatar(),
                req.getPhone(), req.getDefaultAddress());
        return Result.ok();
    }

    /** 储值充值（模拟支付） */
    @PostMapping("/recharge")
    public Result<BigDecimal> recharge(@RequestBody RechargeRequest req) {
        return Result.ok(userService.recharge(UserContext.uid(), req.getAmount()));
    }

    @GetMapping("/balance-records")
    public Result<PageResult<BalanceRecord>> balanceRecords(@RequestParam(defaultValue = "1") long page,
                                                            @RequestParam(defaultValue = "20") long size) {
        return Result.ok(userService.balanceRecords(UserContext.uid(), page, size));
    }

    @GetMapping("/points-records")
    public Result<PageResult<PointsRecord>> pointsRecords(@RequestParam(defaultValue = "1") long page,
                                                          @RequestParam(defaultValue = "20") long size) {
        return Result.ok(userService.pointsRecords(UserContext.uid(), page, size));
    }

    /** 供其他模块查询用户信息 */
    public User getUser(Long uid) {
        return userService.getById(uid);
    }
}
