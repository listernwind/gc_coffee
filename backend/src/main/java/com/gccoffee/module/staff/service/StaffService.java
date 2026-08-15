package com.gccoffee.module.staff.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.gccoffee.common.BizException;
import com.gccoffee.module.coffee.entity.CoffeeReservation;
import com.gccoffee.module.coffee.mapper.CoffeeReservationMapper;
import com.gccoffee.module.coffee.service.CoffeeService;
import com.gccoffee.module.user.entity.User;
import com.gccoffee.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 派送员工作台：任务列表 / 认领 / 派送状态流转
 */
@Service
@RequiredArgsConstructor
public class StaffService {

    private final CoffeeReservationMapper reservationMapper;
    private final CoffeeService coffeeService;
    private final UserMapper userMapper;

    public Map<String, Object> me(Long uid) {
        User u = userMapper.selectById(uid);
        if (u == null || !"STAFF".equals(u.getRole())) {
            throw new BizException("派送员不存在");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("name", u.getNickname());
        m.put("phone", u.getPhone());
        return m;
    }

    /**
     * 派送任务列表
     * @param date today 今天 / tomorrow 明天 / all 全部（今天及以后）
     */
    public List<Map<String, Object>> tasks(String date) {
        LambdaQueryWrapper<CoffeeReservation> qw = new LambdaQueryWrapper<CoffeeReservation>()
                .ne(CoffeeReservation::getStatus, "CANCELLED");
        LocalDate today = LocalDate.now();
        if ("tomorrow".equals(date)) {
            qw.eq(CoffeeReservation::getDeliveryDate, today.plusDays(1));
        } else if ("all".equals(date)) {
            qw.ge(CoffeeReservation::getDeliveryDate, today);
        } else {
            qw.eq(CoffeeReservation::getDeliveryDate, today);
        }
        qw.orderByAsc(CoffeeReservation::getTimeSlot).orderByAsc(CoffeeReservation::getId);
        List<CoffeeReservation> list = reservationMapper.selectList(qw);
        if (list.isEmpty()) {
            return List.of();
        }
        Map<Long, User> users = userMapper.selectBatchIds(
                        list.stream().map(CoffeeReservation::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<Long> staffIds = list.stream().map(CoffeeReservation::getStaffId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, User> staffs = staffIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(staffIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CoffeeReservation r : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("orderNo", r.getOrderNo());
            m.put("productName", r.getProductName());
            m.put("quantity", r.getQuantity());
            m.put("status", r.getStatus());
            m.put("deliveryDate", r.getDeliveryDate().toString());
            m.put("timeSlot", r.getTimeSlot());
            m.put("address", r.getAddress());
            m.put("contactName", r.getContactName());
            m.put("contactPhone", r.getContactPhone());
            m.put("remark", r.getRemark());
            m.put("staffId", r.getStaffId());
            m.put("customer", users.get(r.getUserId()) == null ? "" : users.get(r.getUserId()).getNickname());
            m.put("staffName", r.getStaffId() != null && staffs.get(r.getStaffId()) != null
                    ? staffs.get(r.getStaffId()).getNickname() : "");
            rows.add(m);
        }
        return rows;
    }

    /** 认领：待确认 -> 已确认，同时绑定派送员 */
    @Transactional
    public CoffeeReservation claim(Long staffUid, Long id) {
        int updated = reservationMapper.update(null, new LambdaUpdateWrapper<CoffeeReservation>()
                .eq(CoffeeReservation::getId, id)
                .eq(CoffeeReservation::getStatus, "PENDING")
                .isNull(CoffeeReservation::getStaffId)
                .set(CoffeeReservation::getStatus, "CONFIRMED")
                .set(CoffeeReservation::getStaffId, staffUid));
        if (updated == 0) {
            throw new BizException("该订单已被认领或状态已变化");
        }
        return reservationMapper.selectById(id);
    }

    /** 放弃认领：已确认 -> 待确认，释放派送员 */
    @Transactional
    public CoffeeReservation unclaim(Long staffUid, Long id) {
        int updated = reservationMapper.update(null, new LambdaUpdateWrapper<CoffeeReservation>()
                .eq(CoffeeReservation::getId, id)
                .eq(CoffeeReservation::getStatus, "CONFIRMED")
                .eq(CoffeeReservation::getStaffId, staffUid)
                .set(CoffeeReservation::getStatus, "PENDING")
                .set(CoffeeReservation::getStaffId, null));
        if (updated == 0) {
            throw new BizException("仅可放弃自己认领且未开始派送的订单");
        }
        return reservationMapper.selectById(id);
    }

    /** 派送状态流转（仅限自己认领的单） */
    public CoffeeReservation updateStatus(Long staffUid, Long id, String status) {
        CoffeeReservation r = reservationMapper.selectById(id);
        if (r == null) {
            throw new BizException("预订不存在");
        }
        if (!staffUid.equals(r.getStaffId())) {
            throw new BizException("该订单未认领或已派给其他派送员");
        }
        return coffeeService.adminSetStatus(id, status);
    }
}
