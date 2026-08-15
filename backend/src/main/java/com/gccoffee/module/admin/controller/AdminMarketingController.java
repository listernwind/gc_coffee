package com.gccoffee.module.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gccoffee.common.Result;
import com.gccoffee.module.activity.entity.Activity;
import com.gccoffee.module.activity.entity.Poster;
import com.gccoffee.module.activity.mapper.ActivityMapper;
import com.gccoffee.module.activity.mapper.PosterMapper;
import com.gccoffee.module.coupon.entity.CouponTemplate;
import com.gccoffee.module.coupon.mapper.CouponTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 营销管理：优惠券 / 月度季度活动 / 海报
 */
@RestController
@RequestMapping("/api/admin/marketing")
@RequiredArgsConstructor
public class AdminMarketingController {

    private final CouponTemplateMapper couponMapper;
    private final ActivityMapper activityMapper;
    private final PosterMapper posterMapper;

    // ---------- 优惠券模板 ----------

    @GetMapping("/coupons")
    public Result<List<CouponTemplate>> coupons() {
        return Result.ok(couponMapper.selectList(new LambdaQueryWrapper<CouponTemplate>()
                .orderByDesc(CouponTemplate::getId)));
    }

    @PostMapping("/coupons")
    public Result<CouponTemplate> saveCoupon(@RequestBody CouponTemplate c) {
        if (c.getName() == null || c.getName().isBlank()) {
            throw new com.gccoffee.common.BizException("券名称不能为空");
        }
        if (c.getType() == null || !List.of("FULL_REDUCTION", "DISCOUNT", "CASH").contains(c.getType())) {
            throw new com.gccoffee.common.BizException("券类型不正确");
        }
        if (c.getValue() == null || c.getValue().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new com.gccoffee.common.BizException("券面额/折扣必须大于 0");
        }
        if ("DISCOUNT".equals(c.getType())
                && c.getValue().compareTo(java.math.BigDecimal.ONE) >= 0) {
            throw new com.gccoffee.common.BizException("折扣率必须在 0~1 之间（如 0.9 表示 9 折）");
        }
        if (c.getId() == null) {
            c.setIssuedCount(0);
            c.setCreatedAt(LocalDateTime.now());
            couponMapper.insert(c);
        } else {
            couponMapper.updateById(c);
        }
        return Result.ok(c);
    }

    @DeleteMapping("/coupons/{id}")
    public Result<Void> deleteCoupon(@PathVariable Long id) {
        couponMapper.deleteById(id);
        return Result.ok();
    }

    // ---------- 活动 ----------

    @GetMapping("/activities")
    public Result<List<Activity>> activities() {
        return Result.ok(activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                .orderByAsc(Activity::getType).orderByAsc(Activity::getSort)));
    }

    @PostMapping("/activities")
    public Result<Activity> saveActivity(@RequestBody Activity a) {
        if (a.getTitle() == null || a.getTitle().isBlank()) {
            throw new com.gccoffee.common.BizException("活动标题不能为空");
        }
        if (a.getStartAt() == null || a.getEndAt() == null) {
            throw new com.gccoffee.common.BizException("请填写活动开始/结束日期");
        }
        if (a.getEndAt().isBefore(a.getStartAt())) {
            throw new com.gccoffee.common.BizException("结束日期不能早于开始日期");
        }
        if (a.getId() == null) {
            a.setCreatedAt(LocalDateTime.now());
            activityMapper.insert(a);
        } else {
            activityMapper.updateById(a);
        }
        return Result.ok(a);
    }

    @DeleteMapping("/activities/{id}")
    public Result<Void> deleteActivity(@PathVariable Long id) {
        activityMapper.deleteById(id);
        return Result.ok();
    }

    // ---------- 海报 ----------

    @GetMapping("/posters")
    public Result<List<Poster>> posters() {
        return Result.ok(posterMapper.selectList(new LambdaQueryWrapper<Poster>()
                .orderByAsc(Poster::getSort)));
    }

    @PostMapping("/posters")
    public Result<Poster> savePoster(@RequestBody Poster p) {
        if (p.getId() == null) {
            p.setCreatedAt(LocalDateTime.now());
            posterMapper.insert(p);
        } else {
            posterMapper.updateById(p);
        }
        return Result.ok(p);
    }

    @DeleteMapping("/posters/{id}")
    public Result<Void> deletePoster(@PathVariable Long id) {
        posterMapper.deleteById(id);
        return Result.ok();
    }
}
