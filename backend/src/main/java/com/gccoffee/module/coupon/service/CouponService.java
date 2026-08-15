package com.gccoffee.module.coupon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.gccoffee.common.BizException;
import com.gccoffee.module.coupon.entity.CouponTemplate;
import com.gccoffee.module.coupon.entity.UserCoupon;
import com.gccoffee.module.coupon.mapper.CouponTemplateMapper;
import com.gccoffee.module.coupon.mapper.UserCouponMapper;
import com.gccoffee.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponTemplateMapper templateMapper;
    private final UserCouponMapper userCouponMapper;
    private final UserService userService;

    /** 可领取的优惠券 */
    public List<CouponTemplate> templates() {
        LocalDateTime now = LocalDateTime.now();
        return templateMapper.selectList(new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getActive, 1)
                .and(w -> w.isNull(CouponTemplate::getStartAt).or().le(CouponTemplate::getStartAt, now))
                .and(w -> w.isNull(CouponTemplate::getEndAt).or().ge(CouponTemplate::getEndAt, now))
                .orderByAsc(CouponTemplate::getRedeemPoints).orderByAsc(CouponTemplate::getId));
    }

    @Transactional
    public UserCoupon claim(Long uid, Long templateId) {
        CouponTemplate t = getTemplate(templateId);
        checkClaimable(t);
        long claimed = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, uid).eq(UserCoupon::getTemplateId, templateId));
        if (claimed >= t.getPerUserLimit()) {
            throw new BizException("已达领取上限");
        }
        templateMapper.update(null, new LambdaUpdateWrapper<CouponTemplate>()
                .eq(CouponTemplate::getId, templateId)
                .set(CouponTemplate::getIssuedCount, t.getIssuedCount() + 1));
        return issue(uid, t, "CLAIM");
    }

    @Transactional
    public UserCoupon redeem(Long uid, Long templateId) {
        CouponTemplate t = getTemplate(templateId);
        if (t.getRedeemPoints() == null || t.getRedeemPoints() <= 0) {
            throw new BizException("该优惠券不支持积分兑换");
        }
        checkClaimable(t);
        userService.deductPoints(uid, t.getRedeemPoints(), "CP" + t.getId(), "积分兑换「" + t.getName() + "」");
        templateMapper.update(null, new LambdaUpdateWrapper<CouponTemplate>()
                .eq(CouponTemplate::getId, templateId)
                .set(CouponTemplate::getIssuedCount, t.getIssuedCount() + 1));
        return issue(uid, t, "REDEEM");
    }

    private void checkClaimable(CouponTemplate t) {
        LocalDateTime now = LocalDateTime.now();
        if (t.getActive() == null || t.getActive() != 1) {
            throw new BizException("优惠券已下架");
        }
        if (t.getStartAt() != null && t.getStartAt().isAfter(now)) {
            throw new BizException("活动尚未开始");
        }
        if (t.getEndAt() != null && t.getEndAt().isBefore(now)) {
            throw new BizException("活动已结束");
        }
        if (t.getTotalCount() != null && t.getTotalCount() > 0 && t.getIssuedCount() >= t.getTotalCount()) {
            throw new BizException("已被领完");
        }
    }

    private UserCoupon issue(Long uid, CouponTemplate t, String source) {
        UserCoupon c = new UserCoupon();
        c.setUserId(uid);
        c.setTemplateId(t.getId());
        c.setName(t.getName());
        c.setType(t.getType());
        c.setValue(t.getValue());
        c.setMinAmount(t.getMinAmount() == null ? BigDecimal.ZERO : t.getMinAmount());
        c.setStatus("UNUSED");
        c.setSource(source);
        c.setExpireAt(LocalDateTime.now().plusDays(t.getValidDays() == null ? 7 : t.getValidDays()));
        c.setCreatedAt(LocalDateTime.now());
        userCouponMapper.insert(c);
        return c;
    }

    /** 我的优惠券（自动把过期券置为 EXPIRED） */
    public List<UserCoupon> mine(Long uid, String status) {
        userCouponMapper.update(null, new LambdaUpdateWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, uid).eq(UserCoupon::getStatus, "UNUSED")
                .lt(UserCoupon::getExpireAt, LocalDateTime.now())
                .set(UserCoupon::getStatus, "EXPIRED"));
        LambdaQueryWrapper<UserCoupon> qw = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, uid);
        if (status != null && !status.isBlank() && !"ALL".equals(status)) {
            qw.eq(UserCoupon::getStatus, status);
        }
        qw.orderByDesc(UserCoupon::getId);
        return userCouponMapper.selectList(qw);
    }

    /** 结算页可用券 */
    public List<UserCoupon> usable(Long uid, BigDecimal totalAmount) {
        List<UserCoupon> all = mine(uid, "UNUSED");
        return all.stream().filter(c -> c.getMinAmount() == null
                        || c.getMinAmount().compareTo(totalAmount) <= 0)
                .toList();
    }

    /**
     * 下单时核销优惠券，返回抵扣金额
     */
    @Transactional
    public BigDecimal useCoupon(Long uid, Long couponId, BigDecimal totalAmount, String orderNo) {
        UserCoupon c = userCouponMapper.selectById(couponId);
        if (c == null || !c.getUserId().equals(uid)) {
            throw new BizException("优惠券不存在");
        }
        if (!"UNUSED".equals(c.getStatus())) {
            throw new BizException("优惠券不可用");
        }
        if (c.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BizException("优惠券已过期");
        }
        if (c.getMinAmount() != null && c.getMinAmount().compareTo(totalAmount) > 0) {
            throw new BizException("未满足使用门槛（满 " + c.getMinAmount().stripTrailingZeros().toPlainString() + " 元可用）");
        }
        BigDecimal discount = calcDiscount(c, totalAmount);
        userCouponMapper.update(null, new LambdaUpdateWrapper<UserCoupon>()
                .eq(UserCoupon::getId, c.getId())
                .set(UserCoupon::getStatus, "USED")
                .set(UserCoupon::getUsedAt, LocalDateTime.now())
                .set(UserCoupon::getUsedOrderNo, orderNo));
        return discount;
    }

    /** 取消订单时退回优惠券 */
    @Transactional
    public void restoreCoupon(Long uid, Long couponId) {
        if (couponId == null) {
            return;
        }
        UserCoupon c = userCouponMapper.selectById(couponId);
        if (c != null && c.getUserId().equals(uid) && "USED".equals(c.getStatus())) {
            userCouponMapper.update(null, new LambdaUpdateWrapper<UserCoupon>()
                    .eq(UserCoupon::getId, c.getId())
                    .set(UserCoupon::getStatus, "UNUSED")
                    .set(UserCoupon::getUsedAt, null)
                    .set(UserCoupon::getUsedOrderNo, null));
        }
    }

    public BigDecimal calcDiscount(UserCoupon c, BigDecimal totalAmount) {
        return switch (c.getType()) {
            case "CASH" -> c.getValue().min(totalAmount);
            case "FULL_REDUCTION" -> c.getValue().min(totalAmount);
            case "DISCOUNT" -> {
                BigDecimal rate = c.getValue() == null ? BigDecimal.ONE : c.getValue();
                if (rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(BigDecimal.ONE) >= 0) {
                    yield BigDecimal.ZERO;
                }
                yield totalAmount.multiply(BigDecimal.ONE.subtract(rate)).setScale(2, java.math.RoundingMode.HALF_UP);
            }
            default -> BigDecimal.ZERO;
        };
    }

    public CouponTemplate getTemplate(Long id) {
        CouponTemplate t = templateMapper.selectById(id);
        if (t == null) {
            throw new BizException("优惠券模板不存在");
        }
        return t;
    }
}
