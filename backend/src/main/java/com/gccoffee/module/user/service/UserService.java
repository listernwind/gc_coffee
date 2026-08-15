package com.gccoffee.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gccoffee.common.BizException;
import com.gccoffee.common.PageResult;
import com.gccoffee.module.admin.service.SettingService;
import com.gccoffee.module.user.dto.UserProfileVO;
import com.gccoffee.module.user.entity.BalanceRecord;
import com.gccoffee.module.user.entity.PointsRecord;
import com.gccoffee.module.user.entity.User;
import com.gccoffee.module.user.mapper.BalanceRecordMapper;
import com.gccoffee.module.user.mapper.PointsRecordMapper;
import com.gccoffee.module.user.mapper.UserMapper;
import com.gccoffee.security.JwtUtil;
import com.gccoffee.util.OrderNoUtil;
import com.gccoffee.util.WxUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final BalanceRecordMapper balanceRecordMapper;
    private final PointsRecordMapper pointsRecordMapper;
    private final SettingService settingService;
    private final JwtUtil jwtUtil;
    private final WxUtil wxUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ==================== 登录 ====================

    public Map<String, Object> loginByWx(String code, String nickname, String avatar) {
        String openid = wxUtil.resolveOpenid(code);
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenid, openid));
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setNickname(nickname == null || nickname.isBlank() ? "咖啡好友" + openid.substring(Math.max(0, openid.length() - 4)) : nickname);
            user.setAvatar(avatar == null ? "" : avatar);
            user.setRole("USER");
            user.setBalance(BigDecimal.ZERO);
            user.setPoints(0);
            user.setTotalSpend(BigDecimal.ZERO);
            user.setStatus(1);
            userMapper.insert(user);
        } else if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("账号已被禁用，请联系店主");
        }
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
        String token = jwtUtil.createToken(user.getId(), user.getRole(), user.getNickname(), false);
        return Map.of("token", token, "role", user.getRole());
    }

    public Map<String, Object> adminLogin(String username, String password) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username).eq(User::getRole, "ADMIN"));
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BizException("账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("账号已被禁用");
        }
        String token = jwtUtil.createToken(user.getId(), "ADMIN", user.getNickname(), true);
        return Map.of("token", token, "role", "ADMIN", "nickname", user.getNickname());
    }

    public User getById(Long id) {
        User u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException(401, "用户不存在");
        }
        return u;
    }

    public UserProfileVO profile(Long uid) {
        User u = getById(uid);
        return UserProfileVO.from(u, levelName(u.getTotalSpend()), nextLevelNeed(u.getTotalSpend()));
    }

    public void updateProfile(Long uid, String nickname, String avatar, String phone, String defaultAddress) {
        User u = new User();
        u.setId(uid);
        u.setNickname(nickname);
        u.setAvatar(avatar);
        u.setPhone(phone);
        u.setDefaultAddress(defaultAddress);
        userMapper.updateById(u);
    }

    // ==================== 储值 / 积分 ====================

    @Transactional
    public BigDecimal recharge(Long uid, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || amount.compareTo(new BigDecimal("5000")) > 0) {
            throw new BizException("充值金额需在 0.01~5000 元之间");
        }
        User u = getById(uid);
        BigDecimal balance = u.getBalance().add(amount);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, uid).set(User::getBalance, balance));
        addBalanceRecord(uid, amount, balance, "RECHARGE", OrderNoUtil.recharge(),
                appConfigPayLabel() + "充值");
        return balance;
    }

    /** 扣减余额（消费），余额不足抛异常 */
    @Transactional
    public void deductBalance(Long uid, BigDecimal amount, String bizType, String bizNo, String remark) {
        User u = getById(uid);
        if (u.getBalance().compareTo(amount) < 0) {
            throw new BizException("余额不足，请先充值");
        }
        BigDecimal balance = u.getBalance().subtract(amount);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, uid).set(User::getBalance, balance));
        addBalanceRecord(uid, amount.negate(), balance, bizType, bizNo, remark);
    }

    /** 余额退款（取消订单） */
    @Transactional
    public void refundBalance(Long uid, BigDecimal amount, String bizType, String bizNo, String remark) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        User u = getById(uid);
        BigDecimal balance = u.getBalance().add(amount);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, uid).set(User::getBalance, balance));
        addBalanceRecord(uid, amount, balance, bizType, bizNo, remark);
    }

    public void addBalanceRecord(Long uid, BigDecimal change, BigDecimal balanceAfter, String bizType, String bizNo, String remark) {
        BalanceRecord r = new BalanceRecord();
        r.setUserId(uid);
        r.setChangeAmount(change);
        r.setBalanceAfter(balanceAfter);
        r.setBizType(bizType);
        r.setBizNo(bizNo);
        r.setRemark(remark);
        r.setCreatedAt(LocalDateTime.now());
        balanceRecordMapper.insert(r);
    }

    /** 消费后累计消费金额（等级依据） */
    @Transactional
    public void addSpend(Long uid, BigDecimal amount) {
        User u = getById(uid);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, uid).set(User::getTotalSpend, u.getTotalSpend().add(amount)));
    }

    public int pointsRate() {
        return settingService.getInt(SettingService.KEY_POINTS_RATE, 1);
    }

    /** 按实付金额赠送积分 */
    @Transactional
    public int earnPoints(Long uid, BigDecimal payAmount, String bizNo, String remark) {
        int rate = pointsRate();
        if (rate <= 0 || payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        int pts = payAmount.multiply(BigDecimal.valueOf(rate)).setScale(0, RoundingMode.DOWN).intValue();
        if (pts <= 0) {
            return 0;
        }
        User u = getById(uid);
        int after = u.getPoints() + pts;
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, uid).set(User::getPoints, after));
        PointsRecord r = new PointsRecord();
        r.setUserId(uid);
        r.setChangePoints(pts);
        r.setPointsAfter(after);
        r.setBizType("EARN");
        r.setBizNo(bizNo);
        r.setRemark(remark);
        r.setCreatedAt(LocalDateTime.now());
        pointsRecordMapper.insert(r);
        return pts;
    }

    /** 扣除积分（兑换） */
    @Transactional
    public void deductPoints(Long uid, int points, String bizNo, String remark) {
        User u = getById(uid);
        if (u.getPoints() < points) {
            throw new BizException("积分不足");
        }
        int after = u.getPoints() - points;
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, uid).set(User::getPoints, after));
        PointsRecord r = new PointsRecord();
        r.setUserId(uid);
        r.setChangePoints(-points);
        r.setPointsAfter(after);
        r.setBizType("REDEEM");
        r.setBizNo(bizNo);
        r.setRemark(remark);
        r.setCreatedAt(LocalDateTime.now());
        pointsRecordMapper.insert(r);
    }

    public PageResult<BalanceRecord> balanceRecords(Long uid, long page, long size) {
        Page<BalanceRecord> p = balanceRecordMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<BalanceRecord>().eq(BalanceRecord::getUserId, uid)
                        .orderByDesc(BalanceRecord::getId));
        return PageResult.of(p);
    }

    public PageResult<PointsRecord> pointsRecords(Long uid, long page, long size) {
        Page<PointsRecord> p = pointsRecordMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<PointsRecord>().eq(PointsRecord::getUserId, uid)
                        .orderByDesc(PointsRecord::getId));
        return PageResult.of(p);
    }

    // ==================== 等级 ====================

    public String levelName(BigDecimal spend) {
        return levelInfo(spend)[0];
    }

    public BigDecimal nextLevelNeed(BigDecimal spend) {
        String[] info = levelInfo(spend);
        return info[1] == null ? null : new BigDecimal(info[1]);
    }

    private String[] levelInfo(BigDecimal spend) {
        BigDecimal silver = settingService.getDecimal(SettingService.KEY_LEVEL_SILVER, new BigDecimal("300"));
        BigDecimal gold = settingService.getDecimal(SettingService.KEY_LEVEL_GOLD, new BigDecimal("1000"));
        BigDecimal black = settingService.getDecimal(SettingService.KEY_LEVEL_BLACK, new BigDecimal("3000"));
        if (spend.compareTo(black) >= 0) {
            return new String[]{"黑金会员", null};
        }
        if (spend.compareTo(gold) >= 0) {
            return new String[]{"金卡会员", black.subtract(spend).toPlainString()};
        }
        if (spend.compareTo(silver) >= 0) {
            return new String[]{"银卡会员", gold.subtract(spend).toPlainString()};
        }
        return new String[]{"普通会员", silver.subtract(spend).toPlainString()};
    }

    private String appConfigPayLabel() {
        return "模拟微信支付";
    }

    public List<User> listByIds(java.util.Collection<Long> ids) {
        return userMapper.selectBatchIds(ids);
    }

    // ==================== 管理端 ====================

    /** 店长调整余额（可为负） */
    @Transactional
    public void adminAdjustBalance(Long uid, BigDecimal amount, String remark) {
        User u = getById(uid);
        BigDecimal balance = u.getBalance().add(amount).max(BigDecimal.ZERO);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, uid).set(User::getBalance, balance));
        addBalanceRecord(uid, amount, balance, "ADJUST", OrderNoUtil.recharge(),
                (remark == null || remark.isBlank() ? "店长调整" : "店长调整：" + remark));
    }

    /** 店长调整积分（可为负） */
    @Transactional
    public void adminAdjustPoints(Long uid, int points, String remark) {
        User u = getById(uid);
        int after = Math.max(0, u.getPoints() + points);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, uid).set(User::getPoints, after));
        PointsRecord r = new PointsRecord();
        r.setUserId(uid);
        r.setChangePoints(points);
        r.setPointsAfter(after);
        r.setBizType("ADJUST");
        r.setBizNo(OrderNoUtil.recharge());
        r.setRemark(remark == null || remark.isBlank() ? "店长调整" : "店长调整：" + remark);
        r.setCreatedAt(LocalDateTime.now());
        pointsRecordMapper.insert(r);
    }

    /** 店长启停用会员 */
    public void adminSetStatus(Long uid, Integer status) {
        User u = getById(uid);
        u.setStatus(status == null || status == 0 ? 0 : 1);
        userMapper.updateById(u);
    }
}
