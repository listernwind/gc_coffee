package com.gccoffee.module.staff.controller;

import com.gccoffee.common.Result;
import com.gccoffee.module.coffee.entity.CoffeeReservation;
import com.gccoffee.module.staff.service.StaffService;
import com.gccoffee.security.UserContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 派送员工作台（/api/staff/**，STAFF/ADMIN 角色可访问）
 */
@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        return Result.ok(staffService.me(UserContext.uid()));
    }

    /** 派送任务：date=today(默认)/tomorrow/all */
    @GetMapping("/tasks")
    public Result<List<Map<String, Object>>> tasks(@RequestParam(defaultValue = "today") String date) {
        return Result.ok(staffService.tasks(date));
    }

    @PutMapping("/tasks/{id}/claim")
    public Result<CoffeeReservation> claim(@PathVariable Long id) {
        return Result.ok(staffService.claim(UserContext.uid(), id));
    }

    @PutMapping("/tasks/{id}/unclaim")
    public Result<CoffeeReservation> unclaim(@PathVariable Long id) {
        return Result.ok(staffService.unclaim(UserContext.uid(), id));
    }

    /** status: DELIVERING / DELIVERED */
    @PutMapping("/tasks/{id}/status")
    public Result<CoffeeReservation> status(@PathVariable Long id, @RequestBody StatusReq req) {
        return Result.ok(staffService.updateStatus(UserContext.uid(), id, req.getStatus()));
    }

    @Data
    public static class StatusReq {
        private String status;
    }
}
