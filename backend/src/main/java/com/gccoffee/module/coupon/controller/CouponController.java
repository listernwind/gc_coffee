package com.gccoffee.module.coupon.controller;

import com.gccoffee.common.Result;
import com.gccoffee.module.coupon.entity.CouponTemplate;
import com.gccoffee.module.coupon.entity.UserCoupon;
import com.gccoffee.module.coupon.service.CouponService;
import com.gccoffee.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/templates")
    public Result<List<CouponTemplate>> templates() {
        return Result.ok(couponService.templates());
    }

    @PostMapping("/{id}/claim")
    public Result<UserCoupon> claim(@PathVariable Long id) {
        return Result.ok(couponService.claim(UserContext.uid(), id));
    }

    @PostMapping("/{id}/redeem")
    public Result<UserCoupon> redeem(@PathVariable Long id) {
        return Result.ok(couponService.redeem(UserContext.uid(), id));
    }

    @GetMapping("/mine")
    public Result<List<UserCoupon>> mine(@RequestParam(defaultValue = "ALL") String status) {
        return Result.ok(couponService.mine(UserContext.uid(), status));
    }

    /** 结算页可用券 */
    @GetMapping("/usable")
    public Result<List<UserCoupon>> usable(@RequestParam BigDecimal amount) {
        return Result.ok(couponService.usable(UserContext.uid(), amount));
    }
}
