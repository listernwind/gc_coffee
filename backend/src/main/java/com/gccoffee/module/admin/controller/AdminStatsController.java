package com.gccoffee.module.admin.controller;

import com.gccoffee.common.Result;
import com.gccoffee.module.admin.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 经营看板 + 台账（管理端）
 */
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService statsService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(statsService.overview());
    }

    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(statsService.trend(days));
    }

    @GetMapping("/top")
    public Result<Map<String, Object>> top(@RequestParam(defaultValue = "5") int n) {
        return Result.ok(statsService.topProducts(n));
    }

    /** 台账：type=ALL/DRINK/COFFEE/RECHARGE/PACKAGE */
    @GetMapping("/ledger")
    public Result<Map<String, Object>> ledger(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "ALL") String type) {
        return Result.ok(statsService.ledger(startDate, endDate, type));
    }

    /** 台账 CSV 导出（含 BOM，Excel 可直接打开） */
    @GetMapping(value = "/ledger/csv", produces = "text/csv;charset=UTF-8")
    public String ledgerCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "ALL") String type) {
        return statsService.ledgerCsv(startDate, endDate, type);
    }
}
