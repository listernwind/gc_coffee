package com.gccoffee.module.coffee.controller;

import com.gccoffee.common.PageResult;
import com.gccoffee.common.Result;
import com.gccoffee.module.coffee.dto.MonthlySummaryVO;
import com.gccoffee.module.coffee.dto.PackageBuyRequest;
import com.gccoffee.module.coffee.dto.ReserveRequest;
import com.gccoffee.module.coffee.entity.CoffeePackage;
import com.gccoffee.module.coffee.entity.CoffeeProduct;
import com.gccoffee.module.coffee.entity.CoffeeReservation;
import com.gccoffee.module.coffee.entity.UserPackage;
import com.gccoffee.module.coffee.service.CoffeeService;
import com.gccoffee.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coffee")
@RequiredArgsConstructor
public class CoffeeController {

    private final CoffeeService coffeeService;

    @GetMapping("/products")
    public Result<List<CoffeeProduct>> products() {
        return Result.ok(coffeeService.products());
    }

    @GetMapping("/packages")
    public Result<List<CoffeePackage>> packages() {
        return Result.ok(coffeeService.packages());
    }

    /** 本月套餐额度（个人中心/预订页展示剩余瓶数） */
    @GetMapping("/package/current")
    public Result<UserPackage> currentPackage() {
        return Result.ok(coffeeService.currentPackage(UserContext.uid()));
    }

    /** 次日开始的可用派送日期 */
    @GetMapping("/delivery-days")
    public Result<List<String>> deliveryDays(@RequestParam(defaultValue = "7") int n) {
        return Result.ok(coffeeService.nextDays(n));
    }

    @GetMapping("/delivery-slots")
    public Result<List<String>> deliverySlots() {
        return Result.ok(coffeeService.deliverySlots());
    }

    @PostMapping("/package/buy")
    public Result<UserPackage> buyPackage(@Valid @RequestBody PackageBuyRequest req) {
        return Result.ok(coffeeService.buyPackage(UserContext.uid(), req));
    }

    @PostMapping("/reserve")
    public Result<CoffeeReservation> reserve(@Valid @RequestBody ReserveRequest req) {
        return Result.ok(coffeeService.reserve(UserContext.uid(), req));
    }

    @GetMapping("/reservations")
    public Result<PageResult<CoffeeReservation>> myReservations(@RequestParam(defaultValue = "1") long page,
                                                                @RequestParam(defaultValue = "20") long size) {
        return Result.ok(coffeeService.myReservations(UserContext.uid(), page, size));
    }

    @PostMapping("/reservations/{id}/cancel")
    public Result<CoffeeReservation> cancel(@PathVariable Long id) {
        return Result.ok(coffeeService.cancel(UserContext.uid(), id));
    }

    /** 月度咖啡液详情：花费 / 剩余 / 明细 */
    @GetMapping("/monthly")
    public Result<MonthlySummaryVO> monthly(@RequestParam(required = false) String month) {
        return Result.ok(coffeeService.monthlySummary(UserContext.uid(), month));
    }

    /** 首页展示：本月剩余瓶数等 */
    @GetMapping("/home-card")
    public Result<Map<String, Object>> homeCard() {
        UserPackage up = coffeeService.currentPackage(UserContext.uid());
        int remain = up == null ? 0 : up.getTotalQuota() - up.getUsedQuota();
        return Result.ok(Map.of(
                "hasPackage", up != null,
                "remain", remain,
                "packageName", up == null ? "" : up.getPackageName(),
                "month", up == null ? "" : up.getMonth()
        ));
    }
}
