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

    public List<String> nextDays(int n) {
        List<String> days = new ArrayList<>();
        LocalDate d = LocalDate.now().plusDays(1);
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
        if ("BALANCE".equals(payType)) {
            userService.deductBalance(uid, price, "PACKAGE", OrderNoUtil.packageNo(), "购买月度套餐「" + pkg.getName() + "」");
        }
        // 累计消费 + 积分
        userService.addSpend(uid, price);
        userService.earnPoints(uid, price, OrderNoUtil.packageNo(), "购买月度套餐");

        UserPackage up = new UserPackage();
        up.setUserId(uid);
        up.setPackageId(pkg.getId());
        up.setPackageName(pkg.getName());
        up.setMonth(month);
        up.setTotalQuota(pkg.getBottleCount());
        up.setUsedQuota(0);
        up.setAmount(price);
        up.setCreatedAt(LocalDateTime.now());
        userPackageMapper.insert(up);
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
        LocalDate deliveryDate = req.getDeliveryDate() == null ? LocalDate.now().plusDays(1) : req.getDeliveryDate();
        if (!deliveryDate.isAfter(LocalDate.now())) {
            throw new BizException("咖啡液为次日派送，请选择明天的日期");
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

        if (payQty > 0) {
            if ("BALANCE".equals(payType)) {
                userService.deductBalance(uid, amount, "CONSUME", OrderNoUtil.coffee(), "咖啡液预订 " + quantity + "瓶（含额度抵扣" + packageUsed + "瓶）");
            }
            userService.addSpend(uid, amount);
            userService.earnPoints(uid, amount, OrderNoUtil.coffee(), "咖啡液预订");
        }
        if (packageUsed > 0 && up != null) {
            userPackageMapper.update(null, new LambdaUpdateWrapper<UserPackage>()
                    .eq(UserPackage::getId, up.getId())
                    .set(UserPackage::getUsedQuota, up.getUsedQuota() + packageUsed));
        }

        CoffeeReservation r = new CoffeeReservation();
        r.setOrderNo(OrderNoUtil.coffee());
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

    /** 取消预订：退还按次支付金额 + 恢复套餐额度 */
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
        r.setStatus("CANCELLED");
        r.setCancelAt(LocalDateTime.now());
        reservationMapper.updateById(r);

        if (r.getPayQuantity() != null && r.getPayQuantity() > 0 && r.getAmount() != null
                && r.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            userService.refundBalance(uid, r.getAmount(), "REFUND", r.getOrderNo(), "取消咖啡液预订退款");
        }
        if (r.getPackageUsed() != null && r.getPackageUsed() > 0) {
            String month = r.getCreatedAt().toLocalDate().format(MONTH_FMT);
            UserPackage up = userPackageMapper.selectOne(new LambdaQueryWrapper<UserPackage>()
                    .eq(UserPackage::getUserId, uid).eq(UserPackage::getMonth, month));
            if (up != null) {
                userPackageMapper.update(null, new LambdaUpdateWrapper<UserPackage>()
                        .eq(UserPackage::getId, up.getId())
                        .set(UserPackage::getUsedQuota, Math.max(0, up.getUsedQuota() - r.getPackageUsed())));
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
        String start = month + "-01";
        String end = month + "-31";
        List<CoffeeReservation> records = reservationMapper.selectList(new LambdaQueryWrapper<CoffeeReservation>()
                .eq(CoffeeReservation::getUserId, uid)
                .ge(CoffeeReservation::getCreatedAt, LocalDateTime.parse(start + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .le(CoffeeReservation::getCreatedAt, LocalDateTime.parse(end + " 23:59:59", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
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
        r.setStatus(status);
        reservationMapper.updateById(r);
        return r;
    }

    /** 店长取消预订（退款 + 恢复额度） */
    @Transactional
    public CoffeeReservation adminCancel(Long id) {
        CoffeeReservation r = reservationMapper.selectById(id);
        if (r == null) {
            throw new BizException("预订不存在");
        }
        if ("CANCELLED".equals(r.getStatus()) || "DELIVERED".equals(r.getStatus())) {
            throw new BizException("当前状态不可取消");
        }
        r.setStatus("CANCELLED");
        r.setCancelAt(LocalDateTime.now());
        reservationMapper.updateById(r);
        if (r.getPayQuantity() != null && r.getPayQuantity() > 0 && r.getAmount() != null
                && r.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            userService.refundBalance(r.getUserId(), r.getAmount(), "REFUND", r.getOrderNo(), "店长取消预订退款");
        }
        if (r.getPackageUsed() != null && r.getPackageUsed() > 0) {
            String month = r.getCreatedAt().toLocalDate().format(MONTH_FMT);
            UserPackage up = userPackageMapper.selectOne(new LambdaQueryWrapper<UserPackage>()
                    .eq(UserPackage::getUserId, r.getUserId()).eq(UserPackage::getMonth, month));
            if (up != null) {
                userPackageMapper.update(null, new LambdaUpdateWrapper<UserPackage>()
                        .eq(UserPackage::getId, up.getId())
                        .set(UserPackage::getUsedQuota, Math.max(0, up.getUsedQuota() - r.getPackageUsed())));
            }
        }
        return r;
    }
}
