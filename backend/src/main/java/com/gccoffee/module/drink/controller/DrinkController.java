package com.gccoffee.module.drink.controller;

import com.gccoffee.common.PageResult;
import com.gccoffee.common.Result;
import com.gccoffee.module.drink.dto.DrinkOrderRequest;
import com.gccoffee.module.drink.entity.DrinkOrder;
import com.gccoffee.module.drink.service.DrinkService;
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
@RequestMapping("/api/drink")
@RequiredArgsConstructor
public class DrinkController {

    private final DrinkService drinkService;

    @GetMapping("/menu")
    public Result<List<Map<String, Object>>> menu() {
        return Result.ok(drinkService.menu());
    }

    @PostMapping("/order")
    public Result<DrinkOrder> order(@Valid @RequestBody DrinkOrderRequest req) {
        return Result.ok(drinkService.createOrder(UserContext.uid(), req));
    }

    @GetMapping("/orders")
    public Result<PageResult<DrinkOrder>> myOrders(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "20") long size,
                                                   @RequestParam(defaultValue = "ALL") String status) {
        return Result.ok(drinkService.myOrders(UserContext.uid(), page, size, status));
    }

    @GetMapping("/orders/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(drinkService.detail(UserContext.uid(), id));
    }

    @PostMapping("/orders/{id}/cancel")
    public Result<DrinkOrder> cancel(@PathVariable Long id) {
        return Result.ok(drinkService.cancel(UserContext.uid(), id));
    }
}
