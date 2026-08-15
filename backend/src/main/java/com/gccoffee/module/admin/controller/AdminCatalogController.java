package com.gccoffee.module.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gccoffee.common.Result;
import com.gccoffee.module.coffee.entity.CoffeePackage;
import com.gccoffee.module.coffee.entity.CoffeeProduct;
import com.gccoffee.module.coffee.mapper.CoffeePackageMapper;
import com.gccoffee.module.coffee.mapper.CoffeeProductMapper;
import com.gccoffee.module.drink.entity.DrinkCategory;
import com.gccoffee.module.drink.entity.DrinkProduct;
import com.gccoffee.module.drink.mapper.DrinkCategoryMapper;
import com.gccoffee.module.drink.mapper.DrinkProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品目录管理：饮品分类/饮品/咖啡液/月度套餐
 */
@RestController
@RequestMapping("/api/admin/catalog")
@RequiredArgsConstructor
public class AdminCatalogController {

    private final DrinkCategoryMapper categoryMapper;
    private final DrinkProductMapper drinkProductMapper;
    private final CoffeeProductMapper coffeeProductMapper;
    private final CoffeePackageMapper coffeePackageMapper;

    // ---------- 饮品分类 ----------

    @GetMapping("/drink-categories")
    public Result<List<DrinkCategory>> categories() {
        return Result.ok(categoryMapper.selectList(new LambdaQueryWrapper<DrinkCategory>()
                .orderByAsc(DrinkCategory::getSort)));
    }

    @PostMapping("/drink-categories")
    public Result<DrinkCategory> saveCategory(@RequestBody DrinkCategory c) {
        if (c.getId() == null) {
            c.setCreatedAt(LocalDateTime.now());
            categoryMapper.insert(c);
        } else {
            categoryMapper.updateById(c);
        }
        return Result.ok(c);
    }

    @DeleteMapping("/drink-categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryMapper.deleteById(id);
        return Result.ok();
    }

    // ---------- 饮品商品 ----------

    @GetMapping("/drink-products")
    public Result<List<DrinkProduct>> drinkProducts() {
        return Result.ok(drinkProductMapper.selectList(new LambdaQueryWrapper<DrinkProduct>()
                .orderByAsc(DrinkProduct::getSort)));
    }

    @PostMapping("/drink-products")
    public Result<DrinkProduct> saveDrinkProduct(@RequestBody DrinkProduct p) {
        validateProduct(p.getName(), p.getPrice());
        if (p.getId() == null) {
            p.setCreatedAt(LocalDateTime.now());
            drinkProductMapper.insert(p);
        } else {
            drinkProductMapper.updateById(p);
        }
        return Result.ok(p);
    }

    @DeleteMapping("/drink-products/{id}")
    public Result<Void> deleteDrinkProduct(@PathVariable Long id) {
        drinkProductMapper.deleteById(id);
        return Result.ok();
    }

    // ---------- 咖啡液产品 ----------

    @GetMapping("/coffee-products")
    public Result<List<CoffeeProduct>> coffeeProducts() {
        return Result.ok(coffeeProductMapper.selectList(new LambdaQueryWrapper<CoffeeProduct>()
                .orderByAsc(CoffeeProduct::getSort)));
    }

    @PostMapping("/coffee-products")
    public Result<CoffeeProduct> saveCoffeeProduct(@RequestBody CoffeeProduct p) {
        validateProduct(p.getName(), p.getPrice());
        if (p.getId() == null) {
            p.setCreatedAt(LocalDateTime.now());
            coffeeProductMapper.insert(p);
        } else {
            coffeeProductMapper.updateById(p);
        }
        return Result.ok(p);
    }

    @DeleteMapping("/coffee-products/{id}")
    public Result<Void> deleteCoffeeProduct(@PathVariable Long id) {
        coffeeProductMapper.deleteById(id);
        return Result.ok();
    }

    // ---------- 咖啡液月度套餐 ----------

    @GetMapping("/coffee-packages")
    public Result<List<CoffeePackage>> packages() {
        return Result.ok(coffeePackageMapper.selectList(new LambdaQueryWrapper<CoffeePackage>()
                .orderByAsc(CoffeePackage::getSort)));
    }

    @PostMapping("/coffee-packages")
    public Result<CoffeePackage> savePackage(@RequestBody CoffeePackage p) {
        validateProduct(p.getName(), p.getPrice());
        if (p.getBottleCount() == null || p.getBottleCount() <= 0) {
            throw new com.gccoffee.common.BizException("每月瓶数必须大于 0");
        }
        if (p.getId() == null) {
            p.setCreatedAt(LocalDateTime.now());
            coffeePackageMapper.insert(p);
        } else {
            coffeePackageMapper.updateById(p);
        }
        return Result.ok(p);
    }

    private void validateProduct(String name, java.math.BigDecimal price) {
        if (name == null || name.isBlank()) {
            throw new com.gccoffee.common.BizException("商品名称不能为空");
        }
        if (price == null || price.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new com.gccoffee.common.BizException("价格必须大于 0");
        }
    }

    @DeleteMapping("/coffee-packages/{id}")
    public Result<Void> deletePackage(@PathVariable Long id) {
        coffeePackageMapper.deleteById(id);
        return Result.ok();
    }
}
