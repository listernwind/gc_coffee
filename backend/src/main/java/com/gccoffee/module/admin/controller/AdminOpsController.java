package com.gccoffee.module.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gccoffee.common.BizException;
import com.gccoffee.common.PageResult;
import com.gccoffee.common.Result;
import com.gccoffee.module.admin.service.SettingService;
import com.gccoffee.module.coffee.entity.CoffeeReservation;
import com.gccoffee.module.coffee.mapper.CoffeeReservationMapper;
import com.gccoffee.module.coffee.service.CoffeeService;
import com.gccoffee.module.drink.entity.DrinkOrder;
import com.gccoffee.module.drink.mapper.DrinkOrderMapper;
import com.gccoffee.module.drink.service.DrinkService;
import com.gccoffee.module.user.entity.User;
import com.gccoffee.module.user.mapper.UserMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 运营管理：派送管理 / 饮品订单 / 系统设置
 */
@RestController
@RequestMapping("/api/admin/ops")
@RequiredArgsConstructor
public class AdminOpsController {

    private final CoffeeReservationMapper reservationMapper;
    private final DrinkOrderMapper drinkOrderMapper;
    private final CoffeeService coffeeService;
    private final DrinkService drinkService;
    private final SettingService settingService;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    // ---------- 咖啡液派送管理 ----------

    @GetMapping("/reservations")
    public Result<PageResult<Map<String, Object>>> reservations(@RequestParam(defaultValue = "1") long page,
                                                                @RequestParam(defaultValue = "20") long size,
                                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                                @RequestParam(defaultValue = "ALL") String status) {
        LambdaQueryWrapper<CoffeeReservation> qw = new LambdaQueryWrapper<>();
        if (date != null) {
            qw.eq(CoffeeReservation::getDeliveryDate, date);
        }
        if (!"ALL".equals(status)) {
            qw.eq(CoffeeReservation::getStatus, status);
        }
        qw.orderByAsc(CoffeeReservation::getDeliveryDate).orderByAsc(CoffeeReservation::getId);
        Page<CoffeeReservation> p = reservationMapper.selectPage(new Page<>(page, size), qw);
        Map<Long, User> users = userMap(p.getRecords().stream().map(CoffeeReservation::getUserId).toList());
        List<Map<String, Object>> records = p.getRecords().stream().map(r -> {
            Map<String, Object> m = toMap(r);
            m.put("nickname", nick(users, r.getUserId()));
            return m;
        }).toList();
        return Result.ok(PageResult.of(records, p.getTotal(), p.getCurrent(), p.getSize()));
    }

    /** 状态流转：CONFIRMED / DELIVERING / DELIVERED */
    @PutMapping("/reservations/{id}/status")
    public Result<CoffeeReservation> reservationStatus(@PathVariable Long id, @RequestBody StatusReq req) {
        return Result.ok(coffeeService.adminSetStatus(id, req.getStatus()));
    }

    @PutMapping("/reservations/{id}/cancel")
    public Result<CoffeeReservation> reservationCancel(@PathVariable Long id) {
        return Result.ok(coffeeService.adminCancel(id));
    }

    // ---------- 饮品订单 ----------

    @GetMapping("/orders")
    public Result<PageResult<Map<String, Object>>> orders(@RequestParam(defaultValue = "1") long page,
                                                          @RequestParam(defaultValue = "20") long size,
                                                          @RequestParam(defaultValue = "ALL") String status,
                                                          @RequestParam(required = false) LocalDate date) {
        LambdaQueryWrapper<DrinkOrder> qw = new LambdaQueryWrapper<>();
        if (date != null) {
            qw.ge(DrinkOrder::getCreatedAt, date.atStartOfDay())
                    .lt(DrinkOrder::getCreatedAt, date.plusDays(1).atStartOfDay());
        }
        if (!"ALL".equals(status)) {
            qw.eq(DrinkOrder::getStatus, status);
        }
        qw.orderByDesc(DrinkOrder::getId);
        Page<DrinkOrder> p = drinkOrderMapper.selectPage(new Page<>(page, size), qw);
        Map<Long, User> users = userMap(p.getRecords().stream().map(DrinkOrder::getUserId).toList());
        List<Map<String, Object>> records = p.getRecords().stream().map(o -> {
            Map<String, Object> m = toMap(o);
            m.put("nickname", nick(users, o.getUserId()));
            return m;
        }).toList();
        return Result.ok(PageResult.of(records, p.getTotal(), p.getCurrent(), p.getSize()));
    }

    /** 状态流转：MAKING / READY / FINISHED / CANCELLED */
    @PutMapping("/orders/{id}/status")
    public Result<DrinkOrder> orderStatus(@PathVariable Long id, @RequestBody StatusReq req) {
        return Result.ok(drinkService.adminSetStatus(id, req.getStatus()));
    }

    // ---------- 系统设置 ----------

    @GetMapping("/settings")
    public Result<Map<String, String>> settings() {
        return Result.ok(settingService.getAll());
    }

    @PutMapping("/settings")
    public Result<Void> saveSettings(@RequestBody Map<String, String> map) {
        String slots = map.get(SettingService.KEY_DELIVERY_SLOTS);
        if (slots != null && !slots.isBlank()) {
            try {
                List<?> parsed = objectMapper.readValue(slots, List.class);
                if (parsed.isEmpty()) {
                    throw new BizException("配送时段不能为空，请至少填写一个");
                }
            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                throw new BizException("配送时段格式错误：应为 JSON 数组，如 [\"07:00-09:00\",\"09:00-11:00\"]");
            }
        }
        settingService.putAll(map);
        return Result.ok();
    }

    // ---------- 工具 ----------

    private Map<Long, User> userMap(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private String nick(Map<Long, User> users, Long uid) {
        User u = users.get(uid);
        return u == null ? "用户" + uid : (u.getNickname() == null || u.getNickname().isBlank() ? "用户" + uid : u.getNickname());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object entity) {
        return new LinkedHashMap<>(objectMapper.convertValue(entity, Map.class));
    }

    @Data
    public static class StatusReq {
        private String status;
    }
}
