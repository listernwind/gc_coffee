package com.gccoffee.module.coffee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gccoffee.common.BizException;
import com.gccoffee.common.PageResult;
import com.gccoffee.module.admin.service.SettingService;
import com.gccoffee.module.coffee.dto.MonthlySummaryVO;
import com.gccoffee.module.coffee.dto.PackageBuyRequest;
import com.gccoffee.module.coffee.dto.ReserveRequest;
import com.gccoffee.module.coffee.entity.CoffeePackage;
import com.gccoffee.module.coffee.entity.CoffeeProduct;
import com.gccoffee.module.coffee.entity.CoffeeReservation;
import com.gccoffee.module.coffee.entity.UserPackage;
import com.gccoffee.module.coffee.mapper.CoffeePackageMapper;
import com.gccoffee.module.coffee.mapper.CoffeeProductMapper;
import com.gccoffee.module.coffee.mapper.CoffeeReservationMapper;
import com.gccoffee.module.coffee.mapper.UserPackageMapper;
import com.gccoffee.module.user.entity.User;
import com.gccoffee.module.user.service.UserService;
import com.gccoffee.util.OrderNoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CoffeeService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final CoffeeProductMapper productMapper;
    private final CoffeePackageMapper packageMapper;
    private final UserPackageMapper userPackageMapper;
    private final CoffeeReservationMapper reservationMapper;
    private final UserService userService;
    private final SettingService settingService;

    // ==================== 查询 ====================

    public List<CoffeeProduct> products() {
        return productMapper.selectList(new LambdaQueryWrapper<CoffeeProduct>()
                .eq(CoffeeProduct::getActive, 1).orderByAsc(CoffeeProduct::getSort));
    }

    public List<CoffeePackage> packages() {
        return packageMapper.selectList(new LambdaQueryWrapper<CoffeePackage>()
                .eq(CoffeePackage::getActive, 1).orderByAsc(CoffeePackage::getSort));
    }

    /** 本月套餐额度 */
    public UserPackage currentPackage(Long uid) {
        String month = LocalDate.now().format(MONTH_FMT);
        return userPackageMapper.selectOne(new LambdaQueryWrapper<UserPackage>()
                .eq(UserPackage::getUserId, uid).eq(UserPackage::getMonth, month));
    }

    public List<String> deliverySlots() {
        return settingService.getDeliverySlots();
    }

    /** 最早可派送日期：默认次日；超过当日截止时间（reserve_deadline）则顺延到后天 */
    public LocalDate minDeliveryDate() {
        String deadline = settingService.get(SettingService.KEY_RESERVE_DEADLINE, "22:00");
        try {
            LocalTime deadlineTime = LocalTime.parse(deadline.length() == 5 ? deadline + ":00" : deadline);
            if (LocalTime.now().isAfter(deadlineTime)) {
                return LocalDate.now().plusDays(2);
            }
        } catch (Exception ignored) {
            // 配置非法时按次日处理
        }
        return LocalDate.now().plusDays(1);
    }

    public List<String> nextDays(int n) {
        List<String> days = new ArrayList<>();
        LocalDate d = minDeliveryDate();
        for (int i = 0; i < n; i++) {
            days.add(d.plusDays(i).toString());
        }
        return days;
    }

    // ==================== 购买月度套餐 ====================

    @Transactional
    public UserPackage buyPackage(Long uid, PackageBuyRequest req) {
        CoffeePackage pkg = packageMapper.selectById(req.getPackageId());
        if (pkg == null || pkg.getActive() == null || pkg.getActive() != 1) {
            throw new BizException("套餐不存在或已下架");
        }
        String month = LocalDate.now().format(MONTH_FMT);
        if (currentPackage(uid) != null) {
            throw new BizException("本月已购买过套餐，额度用完后可单独预订");
        }
        BigDecimal price = pkg.getPrice();
        String payType = normalizePayType(req.getPayType());
        String bizNo = OrderNoUtil.packageNo();
        if ("BALANCE".equals(payType)) {
            userService.deductBalance(uid, price, "PACKAGE", bizNo, "购买月度套餐「" + pkg.getName() + "」");
        }
        // 累计消费 + 积分
        userService.addSpend(uid, price);
        userService.earnPoints(uid, price, bizNo, "购买月度套餐");

        UserPackage up = new UserPackage();
        up.setUserId(uid);
        up.setPackageId(pkg.getId());
        up.setPackageName(pkg.getName());
        up.setMonth(month);
        up.setTotalQuota(pkg.getBottleCount());
        up.setUsedQuota(0);
        up.setAmount(price);
        up.setPayType(payType);
        up.setCreatedAt(LocalDateTime.now());
        try {
            userPackageMapper.insert(up);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new BizException("本月已购买过套餐，额度用完后可单独预订");
        }
        return up;
    }

    // ==================== 咖啡液预订（次日派送） ====================

    @Transactional
    public CoffeeReservation reserve(Long uid, ReserveRequest req) {
        CoffeeProduct product = productMapper.selectById(req.getProductId());
        if (product == null || product.getActive() == null || product.getActive() != 1) {
            throw new BizException("产品不存在或已下架");
        }
        int quantity = req.getQuantity() == null ? 0 : req.getQuantity();
        if (quantity <= 0) {
            throw new BizException("数量至少为 1");
        }
        LocalDate deliveryDate = req.getDeliveryDate() == null ? minDeliveryDate() : req.getDeliveryDate();
        if (deliveryDate.isBefore(minDeliveryDate())) {
            throw new BizException("咖啡液为次日派送，请选择" + minDeliveryDate() + "及之后的日期");
        }
        List<String> slots = deliverySlots();
        if (slots.isEmpty() || !slots.contains(req.getTimeSlot())) {
            throw new BizException("派送时段无效");
        }

        // 套餐额度优先
        int packageUsed = 0;
        UserPackage up = null;
        if (Boolean.TRUE.equals(req.getUsePackage())) {
            up = currentPackage(uid);
            if (up != null) {
                int remain = up.getTotalQuota() - up.getUsedQuota();
                packageUsed = Math.min(quantity, Math.max(0, remain));
            }
        }
        int payQty = quantity - packageUsed;
        BigDecimal amount = product.getPrice().multiply(BigDecimal.valueOf(payQty));
        String payType = normalizePayType(req.getPayType());
        String orderNo = OrderNoUtil.coffee();

        if (payQty > 0) {
            if ("BALANCE".equals(payType)) {
                userService.deductBalance(uid, amount, "CONSUME", orderNo, "咖啡液预订 " + quantity + "瓶（含额度抵扣" + packageUsed + "瓶）");
            }
            userService.addSpend(uid, amount);
            userService.earnPoints(uid, amount, orderNo, "咖啡液预订");
        }
        if (packageUsed > 0 && up != null) {
            int updated = userPackageMapper.update(null, new LambdaUpdateWrapper<UserPackage>()
                    .eq(UserPackage::getId, up.getId())
                    .apply("used_quota + {0} <= total_quota", packageUsed)
                    .setSql("used_quota = used_quota + " + packageUsed));
            if (updated == 0) {
                throw new BizException("套餐剩余额度不足，请减少数量或取消勾选额度抵扣");
            }
        }

        CoffeeReservation r = new CoffeeReservation();
        r.setOrderNo(orderNo);
        r.setUserId(uid);
        r.setProductId(product.getId());
        r.setProductName(product.getName());
        r.setQuantity(quantity);
        r.setPackageUsed(packageUsed);
        r.setPayQuantity(payQty);
        r.setUnitPrice(product.getPrice());
        r.setAmount(amount);
        r.setPayType(payType);
        r.setStatus("PENDING");
        r.setDeliveryDate(deliveryDate);
        r.setTimeSlot(req.getTimeSlot());
        r.setAddress(req.getAddress());
        r.setContactName(req.getContactName());
        r.setContactPhone(req.getContactPhone());
        r.setRemark(req.getRemark());
        r.setCreatedAt(LocalDateTime.now());
        reservationMapper.insert(r);
        return r;
    }

    /** 取消预订：按支付方式退款（余额支付退回余额；微信模拟支付不产生余额变动）+ 回滚积分/累计消费 + 恢复套餐额度 */
    @Transactional
    public CoffeeReservation cancel(Long uid, Long id) {
        CoffeeReservation r = reservationMapper.selectById(id);
        if (r == null || !r.getUserId().equals(uid)) {
            throw new BizException("预订不存在");
        }
        if (!"PENDING".equals(r.getStatus())) {
            throw new BizException("当前状态不可取消");
        }
        if (!r.getDeliveryDate().isAfter(LocalDate.now())) {
            throw new BizException("今天派送的订单已无法取消，请联系店主");
        }
        return doCancel(r);
    }

    /** 取消核心逻辑（用户/店长共用）：退款 + 回滚 + 恢复额度 */
    @Transactional
    private CoffeeReservation doCancel(CoffeeReservation r) {
        reservationMapper.update(null, new LambdaUpdateWrapper<CoffeeReservation>()
                .eq(CoffeeReservation::getId, r.getId())
                .set(CoffeeReservation::getStatus, "CANCELLED")
                .set(CoffeeReservation::getCancelAt, LocalDateTime.now()));
        r.setStatus("CANCELLED");
        r.setCancelAt(LocalDateTime.now());

        if (r.getPayQuantity() != null && r.getPayQuantity() > 0 && r.getAmount() != null
                && r.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            if ("BALANCE".equals(r.getPayType())) {
                userService.refundBalance(r.getUserId(), r.getAmount(), "REFUND", r.getOrderNo(), "取消咖啡液预订退款");
            }
            userService.rollbackSpend(r.getUserId(), r.getAmount());
            userService.rollbackPoints(r.getUserId(),
                    r.getAmount().multiply(BigDecimal.valueOf(userService.pointsRate()))
                            .setScale(0, java.math.RoundingMode.DOWN).intValue(),
                    r.getOrderNo(), "取消咖啡液预订回滚积分");
        }
        if (r.getPackageUsed() != null && r.getPackageUsed() > 0) {
            String month = r.getCreatedAt().toLocalDate().format(MONTH_FMT);
            UserPackage up = userPackageMapper.selectOne(new LambdaQueryWrapper<UserPackage>()
                    .eq(UserPackage::getUserId, r.getUserId()).eq(UserPackage::getMonth, month));
            if (up != null) {
                userPackageMapper.update(null, new LambdaUpdateWrapper<UserPackage>()
                        .eq(UserPackage::getId, up.getId())
                        .setSql("used_quota = GREATEST(used_quota - " + r.getPackageUsed() + ", 0)"));
            }
        }
        return r;
    }

    public PageResult<CoffeeReservation> myReservations(Long uid, long page, long size) {
        Page<CoffeeReservation> p = reservationMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<CoffeeReservation>().eq(CoffeeReservation::getUserId, uid)
                        .orderByDesc(CoffeeReservation::getId));
        return PageResult.of(p);
    }

    /** 月度详情：本月花费 / 剩余 / 明细 */
    public MonthlySummaryVO monthlySummary(Long uid, String month) {
        if (month == null || month.isBlank()) {
            month = LocalDate.now().format(MONTH_FMT);
        }
        YearMonth ym;
        try {
            ym = YearMonth.parse(month);
        } catch (Exception e) {
            throw new BizException("月份格式应为 yyyy-MM");
        }
        List<UserPackage> ups = userPackageMapper.selectList(new LambdaQueryWrapper<UserPackage>()
                .eq(UserPackage::getUserId, uid).eq(UserPackage::getMonth, month));
        List<MonthlySummaryVO.PackageCardVO> cards = new ArrayList<>();
        int quotaTotal = 0, quotaUsed = 0;
        BigDecimal packageCost = BigDecimal.ZERO;
        for (UserPackage up : ups) {
            quotaTotal += up.getTotalQuota();
            quotaUsed += up.getUsedQuota();
            packageCost = packageCost.add(up.getAmount());
            MonthlySummaryVO.PackageCardVO card = new MonthlySummaryVO.PackageCardVO();
            card.setId(up.getId());
            card.setPackageName(up.getPackageName());
            card.setTotalQuota(up.getTotalQuota());
            card.setUsedQuota(up.getUsedQuota());
            card.setRemain(up.getTotalQuota() - up.getUsedQuota());
            card.setAmount(up.getAmount());
            cards.add(card);
        }
        // 用下月 1 日 0 点作为开区间上界，天然兼容 2 月/大小月
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();
        List<CoffeeReservation> records = reservationMapper.selectList(new LambdaQueryWrapper<CoffeeReservation>()
                .eq(CoffeeReservation::getUserId, uid)
                .ge(CoffeeReservation::getCreatedAt, start)
                .lt(CoffeeReservation::getCreatedAt, end)
                .orderByDesc(CoffeeReservation::getId));
        BigDecimal paid = records.stream()
                .filter(r -> !"CANCELLED".equals(r.getStatus()))
                .map(CoffeeReservation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        MonthlySummaryVO vo = new MonthlySummaryVO();
        vo.setMonth(month);
        vo.setSpend(packageCost.add(paid));
        vo.setQuotaTotal(quotaTotal);
        vo.setQuotaUsed(quotaUsed);
        vo.setQuotaRemain(quotaTotal - quotaUsed);
        vo.setPackages(cards);
        vo.setRecords(records);
        return vo;
    }

    private String normalizePayType(String payType) {
        return "BALANCE".equals(payType) ? "BALANCE" : "WX_MOCK";
    }

    // ==================== 管理端 ====================

    /** 店长更新派送状态：PENDING -> CONFIRMED -> DELIVERING -> DELIVERED */
    public CoffeeReservation adminSetStatus(Long id, String status) {
        CoffeeReservation r = reservationMapper.selectById(id);
        if (r == null) {
            throw new BizException("预订不存在");
        }
        String cur = r.getStatus();
        boolean ok = switch (status) {
            case "CONFIRMED" -> "PENDING".equals(cur);
            case "DELIVERING" -> "CONFIRMED".equals(cur);
            case "DELIVERED" -> "DELIVERING".equals(cur);
            default -> false;
        };
        if (!ok) {
            throw new BizException("当前状态「" + cur + "」不能变更为「" + status + "」");
        }
        reservationMapper.update(null, new LambdaUpdateWrapper<CoffeeReservation>()
                .eq(CoffeeReservation::getId, id)
                .set(CoffeeReservation::getStatus, status));
        r.setStatus(status);
        return r;
    }

    /** 店长取消预订（退款 + 回滚积分/累计消费 + 恢复额度） */
    @Transactional
    public CoffeeReservation adminCancel(Long id) {
        CoffeeReservation r = reservationMapper.selectById(id);
        if (r == null) {
            throw new BizException("预订不存在");
        }
        if ("CANCELLED".equals(r.getStatus()) || "DELIVERED".equals(r.getStatus())) {
            throw new BizException("当前状态不可取消");
        }
        return doCancel(r);
    }
}
