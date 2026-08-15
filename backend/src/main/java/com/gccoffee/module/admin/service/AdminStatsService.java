package com.gccoffee.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gccoffee.module.coffee.entity.CoffeeReservation;
import com.gccoffee.module.coffee.entity.UserPackage;
import com.gccoffee.module.coffee.mapper.CoffeeReservationMapper;
import com.gccoffee.module.coffee.mapper.UserPackageMapper;
import com.gccoffee.module.drink.entity.DrinkOrder;
import com.gccoffee.module.drink.entity.DrinkOrderItem;
import com.gccoffee.module.drink.mapper.DrinkOrderItemMapper;
import com.gccoffee.module.drink.mapper.DrinkOrderMapper;
import com.gccoffee.module.user.entity.BalanceRecord;
import com.gccoffee.module.user.entity.User;
import com.gccoffee.module.user.mapper.BalanceRecordMapper;
import com.gccoffee.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 台账统计：经营看板 + 销售台账 + 趋势 + 畅销商品 + CSV 导出
 */
@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DrinkOrderMapper drinkOrderMapper;
    private final DrinkOrderItemMapper drinkOrderItemMapper;
    private final CoffeeReservationMapper reservationMapper;
    private final UserPackageMapper userPackageMapper;
    private final BalanceRecordMapper balanceRecordMapper;
    private final UserMapper userMapper;

    // ==================== 经营看板 ====================

    public Map<String, Object> overview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        BigDecimal todayDrink = sum(drinkOrderMapper.selectList(new LambdaQueryWrapper<DrinkOrder>()
                .ne(DrinkOrder::getStatus, "CANCELLED").ge(DrinkOrder::getPaidAt, todayStart)
                .select(DrinkOrder::getPayAmount)), DrinkOrder::getPayAmount);
        BigDecimal todayCoffee = sum(reservationMapper.selectList(new LambdaQueryWrapper<CoffeeReservation>()
                .ne(CoffeeReservation::getStatus, "CANCELLED").ge(CoffeeReservation::getCreatedAt, todayStart)
                .select(CoffeeReservation::getAmount)), CoffeeReservation::getAmount);
        BigDecimal todayRecharge = sum(balanceRecordMapper.selectList(new LambdaQueryWrapper<BalanceRecord>()
                .eq(BalanceRecord::getBizType, "RECHARGE").ge(BalanceRecord::getCreatedAt, todayStart)
                .select(BalanceRecord::getChangeAmount)), BalanceRecord::getChangeAmount);
        BigDecimal todayPackage = sum(userPackageMapper.selectList(new LambdaQueryWrapper<UserPackage>()
                .ge(UserPackage::getCreatedAt, todayStart).select(UserPackage::getAmount)), UserPackage::getAmount);

        long todayDrinkCount = drinkOrderMapper.selectCount(new LambdaQueryWrapper<DrinkOrder>()
                .ne(DrinkOrder::getStatus, "CANCELLED").ge(DrinkOrder::getPaidAt, todayStart));
        long todayReserveCount = reservationMapper.selectCount(new LambdaQueryWrapper<CoffeeReservation>()
                .ne(CoffeeReservation::getStatus, "CANCELLED").ge(CoffeeReservation::getCreatedAt, todayStart));

        BigDecimal monthDrink = sum(drinkOrderMapper.selectList(new LambdaQueryWrapper<DrinkOrder>()
                .ne(DrinkOrder::getStatus, "CANCELLED").ge(DrinkOrder::getPaidAt, monthStart)
                .select(DrinkOrder::getPayAmount)), DrinkOrder::getPayAmount);
        BigDecimal monthCoffee = sum(reservationMapper.selectList(new LambdaQueryWrapper<CoffeeReservation>()
                .ne(CoffeeReservation::getStatus, "CANCELLED").ge(CoffeeReservation::getCreatedAt, monthStart)
                .select(CoffeeReservation::getAmount)), CoffeeReservation::getAmount);
        BigDecimal monthRecharge = sum(balanceRecordMapper.selectList(new LambdaQueryWrapper<BalanceRecord>()
                .eq(BalanceRecord::getBizType, "RECHARGE").ge(BalanceRecord::getCreatedAt, monthStart)
                .select(BalanceRecord::getChangeAmount)), BalanceRecord::getChangeAmount);
        BigDecimal monthPackage = sum(userPackageMapper.selectList(new LambdaQueryWrapper<UserPackage>()
                .ge(UserPackage::getCreatedAt, monthStart).select(UserPackage::getAmount)), UserPackage::getAmount);

        long memberCount = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "USER"));
        long pendingDelivery = reservationMapper.selectCount(new LambdaQueryWrapper<CoffeeReservation>()
                .ge(CoffeeReservation::getDeliveryDate, LocalDate.now())
                .in(CoffeeReservation::getStatus, List.of("PENDING", "CONFIRMED", "DELIVERING")));
        long makingOrders = drinkOrderMapper.selectCount(new LambdaQueryWrapper<DrinkOrder>()
                .in(DrinkOrder::getStatus, List.of("PAID", "MAKING")));

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("todaySales", todayDrink.add(todayCoffee).add(todayRecharge).add(todayPackage));
        m.put("todayDrink", todayDrink);
        m.put("todayCoffee", todayCoffee);
        m.put("todayRecharge", todayRecharge);
        m.put("todayOrders", todayDrinkCount + todayReserveCount);
        m.put("todayReservations", todayReserveCount);
        m.put("monthSales", monthDrink.add(monthCoffee).add(monthRecharge).add(monthPackage));
        m.put("memberCount", memberCount);
        m.put("pendingDelivery", pendingDelivery);
        m.put("makingOrders", makingOrders);
        m.put("today", LocalDate.now().toString());
        return m;
    }

    /** 近 N 天每日销售额与订单数 */
    public List<Map<String, Object>> trend(int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(Math.min(Math.max(days, 1), 90) - 1L);
        LocalDateTime s = start.atStartOfDay();
        LocalDateTime e = end.plusDays(1).atStartOfDay();

        Map<String, BigDecimal> sales = new LinkedHashMap<>();
        Map<String, Long> counts = new LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            sales.put(d.toString(), BigDecimal.ZERO);
            counts.put(d.toString(), 0L);
        }
        addDaily(sales, counts, drinkOrderMapper.selectList(new LambdaQueryWrapper<DrinkOrder>()
                        .ne(DrinkOrder::getStatus, "CANCELLED").ge(DrinkOrder::getPaidAt, s).lt(DrinkOrder::getPaidAt, e)),
                DrinkOrder::getPaidAt, DrinkOrder::getPayAmount);
        addDaily(sales, counts, reservationMapper.selectList(new LambdaQueryWrapper<CoffeeReservation>()
                        .ne(CoffeeReservation::getStatus, "CANCELLED").ge(CoffeeReservation::getCreatedAt, s).lt(CoffeeReservation::getCreatedAt, e)),
                CoffeeReservation::getCreatedAt, CoffeeReservation::getAmount);
        addDaily(sales, counts, balanceRecordMapper.selectList(new LambdaQueryWrapper<BalanceRecord>()
                        .eq(BalanceRecord::getBizType, "RECHARGE").ge(BalanceRecord::getCreatedAt, s).lt(BalanceRecord::getCreatedAt, e)),
                BalanceRecord::getCreatedAt, BalanceRecord::getChangeAmount);
        addDaily(sales, counts, userPackageMapper.selectList(new LambdaQueryWrapper<UserPackage>()
                        .ge(UserPackage::getCreatedAt, s).lt(UserPackage::getCreatedAt, e)),
                UserPackage::getCreatedAt, UserPackage::getAmount);

        List<Map<String, Object>> list = new ArrayList<>();
        sales.forEach((date, v) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", date);
            m.put("sales", v);
            m.put("orders", counts.get(date));
            list.add(m);
        });
        return list;
    }

    /** 畅销商品 TOP N（饮品 + 咖啡液） */
    public Map<String, Object> topProducts(int n) {
        Map<String, Long> drinkQty = new LinkedHashMap<>();
        drinkOrderItemMapper.selectList(new LambdaQueryWrapper<>()).forEach(i ->
                drinkQty.merge(i.getProductName(), (long) i.getQuantity(), Long::sum));
        Map<String, Long> coffeeQty = new LinkedHashMap<>();
        reservationMapper.selectList(new LambdaQueryWrapper<CoffeeReservation>()
                        .ne(CoffeeReservation::getStatus, "CANCELLED"))
                .forEach(r -> coffeeQty.merge(r.getProductName(), (long) r.getQuantity(), Long::sum));

        List<Map<String, Object>> drinks = top(drinkQty, n);
        List<Map<String, Object>> coffees = top(coffeeQty, n);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("drinks", drinks);
        m.put("coffees", coffees);
        return m;
    }

    // ==================== 台账 ====================

    public Map<String, Object> ledger(LocalDate startDate, LocalDate endDate, String type) {
        LocalDate start = startDate == null ? LocalDate.now().withDayOfMonth(1) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        if (start.isAfter(end)) {
            throw new com.gccoffee.common.BizException("开始日期不能晚于结束日期");
        }
        LocalDateTime s = start.atStartOfDay();
        LocalDateTime e = end.plusDays(1).atStartOfDay();
        String t = type == null || type.isBlank() ? "ALL" : type;

        List<Map<String, Object>> rows = new ArrayList<>();
        if ("ALL".equals(t) || "DRINK".equals(t)) {
            rows.addAll(drinkRows(s, e));
        }
        if ("ALL".equals(t) || "COFFEE".equals(t)) {
            rows.addAll(coffeeRows(s, e));
        }
        if ("ALL".equals(t) || "RECHARGE".equals(t)) {
            rows.addAll(rechargeRows(s, e));
        }
        if ("ALL".equals(t) || "PACKAGE".equals(t)) {
            rows.addAll(packageRows(s, e));
        }
        rows.sort(Comparator.comparing((Map<String, Object> r) -> (String) r.get("time")).reversed());
        List<Map<String, Object>> capped = rows.size() > 500 ? new ArrayList<>(rows.subList(0, 500)) : rows;

        BigDecimal income = rows.stream()
                .filter(r -> !"CANCELLED".equals(r.get("status")))
                .map(r -> (BigDecimal) r.get("amount"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("income", income);
        summary.put("count", rows.size());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rows", capped);
        m.put("summary", summary);
        return m;
    }

    public String ledgerCsv(LocalDate startDate, LocalDate endDate, String type) {
        Map<String, Object> ledger = ledger(startDate, endDate, type);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) ledger.get("rows");
        StringBuilder sb = new StringBuilder("\uFEFF"); // UTF-8 BOM，Excel 直接打开不乱码
        sb.append("时间,单号,类型,用户,内容,支付方式,金额,状态\n");
        for (Map<String, Object> r : rows) {
            sb.append(r.get("time")).append(',')
                    .append(r.get("orderNo")).append(',')
                    .append(r.get("type")).append(',')
                    .append(String.valueOf(r.get("user")).replace(",", " ")).append(',')
                    .append(String.valueOf(r.get("content")).replace(",", " ")).append(',')
                    .append(r.get("payType")).append(',')
                    .append(r.get("amount")).append(',')
                    .append(r.get("status")).append('\n');
        }
        return sb.toString();
    }

    private List<Map<String, Object>> drinkRows(LocalDateTime s, LocalDateTime e) {
        List<DrinkOrder> orders = drinkOrderMapper.selectList(new LambdaQueryWrapper<DrinkOrder>()
                .ge(DrinkOrder::getCreatedAt, s).lt(DrinkOrder::getCreatedAt, e));
        if (orders.isEmpty()) {
            return List.of();
        }
        Map<Long, List<DrinkOrderItem>> itemMap = drinkOrderItemMapper.selectList(
                        new LambdaQueryWrapper<DrinkOrderItem>()
                                .in(DrinkOrderItem::getOrderId, orders.stream().map(DrinkOrder::getId).toList()))
                .stream().collect(Collectors.groupingBy(DrinkOrderItem::getOrderId));
        Map<Long, User> users = userMap(orders.stream().map(DrinkOrder::getUserId).toList());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (DrinkOrder o : orders) {
            String content = itemMap.getOrDefault(o.getId(), List.of()).stream()
                    .map(i -> i.getProductName() + "x" + i.getQuantity())
                    .collect(Collectors.joining(" "));
            rows.add(row(o.getCreatedAt(), o.getOrderNo(), "饮品", user(users, o.getUserId()),
                    content, o.getPayType(), o.getPayAmount(), o.getStatus()));
        }
        return rows;
    }

    private List<Map<String, Object>> coffeeRows(LocalDateTime s, LocalDateTime e) {
        List<CoffeeReservation> list = reservationMapper.selectList(new LambdaQueryWrapper<CoffeeReservation>()
                .ge(CoffeeReservation::getCreatedAt, s).lt(CoffeeReservation::getCreatedAt, e));
        if (list.isEmpty()) {
            return List.of();
        }
        Map<Long, User> users = userMap(list.stream().map(CoffeeReservation::getUserId).toList());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CoffeeReservation r : list) {
            String content = r.getProductName() + "x" + r.getQuantity()
                    + (r.getPackageUsed() != null && r.getPackageUsed() > 0 ? "（额度抵" + r.getPackageUsed() + "瓶）" : "")
                    + " " + r.getDeliveryDate() + " " + r.getTimeSlot();
            rows.add(row(r.getCreatedAt(), r.getOrderNo(), "咖啡液", user(users, r.getUserId()),
                    content, r.getPayType(), r.getAmount(), r.getStatus()));
        }
        return rows;
    }

    private List<Map<String, Object>> rechargeRows(LocalDateTime s, LocalDateTime e) {
        List<BalanceRecord> list = balanceRecordMapper.selectList(new LambdaQueryWrapper<BalanceRecord>()
                .eq(BalanceRecord::getBizType, "RECHARGE").ge(BalanceRecord::getCreatedAt, s).lt(BalanceRecord::getCreatedAt, e));
        if (list.isEmpty()) {
            return List.of();
        }
        Map<Long, User> users = userMap(list.stream().map(BalanceRecord::getUserId).toList());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (BalanceRecord r : list) {
            rows.add(row(r.getCreatedAt(), r.getBizNo(), "充值", user(users, r.getUserId()),
                    "余额充值", "WX_MOCK", r.getChangeAmount(), "OK"));
        }
        return rows;
    }

    private List<Map<String, Object>> packageRows(LocalDateTime s, LocalDateTime e) {
        List<UserPackage> list = userPackageMapper.selectList(new LambdaQueryWrapper<UserPackage>()
                .ge(UserPackage::getCreatedAt, s).lt(UserPackage::getCreatedAt, e));
        if (list.isEmpty()) {
            return List.of();
        }
        Map<Long, User> users = userMap(list.stream().map(UserPackage::getUserId).toList());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (UserPackage p : list) {
            rows.add(row(p.getCreatedAt(), "PKG" + p.getId(), "套餐", user(users, p.getUserId()),
                    p.getPackageName() + "（" + p.getMonth() + " " + p.getTotalQuota() + "瓶）",
                    "WX_MOCK", p.getAmount(), "OK"));
        }
        return rows;
    }

    private Map<String, Object> row(LocalDateTime time, String orderNo, String type, String user,
                                    String content, String payType, BigDecimal amount, String status) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("time", time.format(DATETIME_FMT));
        m.put("orderNo", orderNo);
        m.put("type", type);
        m.put("user", user);
        m.put("content", content);
        m.put("payType", payType);
        m.put("amount", amount == null ? BigDecimal.ZERO : amount);
        m.put("status", status);
        return m;
    }

    private Map<Long, User> userMap(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private String user(Map<Long, User> users, Long uid) {
        User u = users.get(uid);
        return u == null ? "用户" + uid : (u.getNickname() == null || u.getNickname().isBlank() ? u.getPhone() : u.getNickname());
    }

    private <T> void addDaily(Map<String, BigDecimal> sales, Map<String, Long> counts,
                              List<T> list, Function<T, LocalDateTime> timeFn, Function<T, BigDecimal> amountFn) {
        for (T item : list) {
            String d = timeFn.apply(item).format(DATE_FMT);
            if (sales.containsKey(d)) {
                BigDecimal v = amountFn.apply(item);
                sales.put(d, sales.get(d).add(v == null ? BigDecimal.ZERO : v));
                counts.put(d, counts.get(d) + 1);
            }
        }
    }

    private <T> BigDecimal sum(List<T> list, Function<T, BigDecimal> fn) {
        return list.stream().map(fn).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Map<String, Object>> top(Map<String, Long> qty, int n) {
        return qty.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(n)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", e.getKey());
                    m.put("quantity", e.getValue());
                    return m;
                }).toList();
    }
}
