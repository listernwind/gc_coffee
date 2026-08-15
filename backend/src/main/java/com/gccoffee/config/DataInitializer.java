package com.gccoffee.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gccoffee.module.admin.entity.Setting;
import com.gccoffee.module.admin.mapper.SettingMapper;
import com.gccoffee.module.admin.service.SettingService;
import com.gccoffee.module.coffee.mapper.UserPackageMapper;
import com.gccoffee.module.user.entity.User;
import com.gccoffee.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 启动初始化：默认店长账号、演示用户、默认系统设置
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final UserPackageMapper userPackageMapper;
    private final SettingMapper settingMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) {
        ensureAdmin();
        ensureDemoUser();
        ensureStaff();
        ensureSettings();
        log.info("数据初始化完成");
    }

    private void ensureAdmin() {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, "admin"));
        if (count > 0) {
            return;
        }
        User admin = new User();
        admin.setOpenid("admin_openid");
        admin.setUsername("admin");
        admin.setPassword(new BCryptPasswordEncoder().encode("admin123"));
        admin.setNickname("店长");
        admin.setRole("ADMIN");
        admin.setBalance(BigDecimal.ZERO);
        admin.setPoints(0);
        admin.setTotalSpend(BigDecimal.ZERO);
        admin.setStatus(1);
        admin.setCreatedAt(LocalDateTime.now());
        userMapper.insert(admin);
        log.info("已创建默认店长账号：admin / admin123（请尽快修改）");
    }

    private void ensureDemoUser() {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getOpenid, "mock_demo"));
        if (count > 0) {
            return;
        }
        User demo = new User();
        demo.setOpenid("mock_demo");
        demo.setNickname("演示用户");
        demo.setRole("USER");
        demo.setBalance(new BigDecimal("200"));
        demo.setPoints(500);
        demo.setTotalSpend(new BigDecimal("268"));
        demo.setDefaultAddress("3栋2单元501");
        demo.setPhone("13800000000");
        demo.setStatus(1);
        demo.setCreatedAt(LocalDateTime.now());
        userMapper.insert(demo);
        // 给演示用户配一张本月套餐（额度 30 瓶，已用 5 瓶）
        com.gccoffee.module.coffee.entity.UserPackage up = new com.gccoffee.module.coffee.entity.UserPackage();
        up.setUserId(demo.getId());
        up.setPackageId(2L);
        up.setPackageName("畅饮月卡");
        up.setMonth(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        up.setTotalQuota(30);
        up.setUsedQuota(5);
        up.setAmount(new BigDecimal("360"));
        up.setPayType("WX_MOCK");
        up.setCreatedAt(LocalDateTime.now());
        userPackageMapper.insert(up);
        log.info("已创建演示用户：mock_demo（余额200元/500积分/本月畅饮月卡剩25瓶）");
    }

    private void ensureStaff() {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, "staff1"));
        if (count > 0) {
            return;
        }
        User staff = new User();
        staff.setOpenid("staff_openid1");
        staff.setUsername("staff1");
        staff.setPassword(new BCryptPasswordEncoder().encode("staff123"));
        staff.setNickname("配送员小王");
        staff.setPhone("13900000000");
        staff.setRole("STAFF");
        staff.setBalance(BigDecimal.ZERO);
        staff.setPoints(0);
        staff.setTotalSpend(BigDecimal.ZERO);
        staff.setStatus(1);
        staff.setCreatedAt(LocalDateTime.now());
        userMapper.insert(staff);
        log.info("已创建默认派送员账号：staff1 / staff123");
    }

    private void ensureSettings() {
        Map<String, String> defaults = Map.ofEntries(
                Map.entry(SettingService.KEY_SHOP_NAME, "GC Coffee 咖啡小馆"),
                Map.entry(SettingService.KEY_SHOP_NOTICE, "欢迎光临 ☕ 咖啡液今日预订、次日送达；饮品到店自取，下单后约10分钟出杯。"),
                Map.entry(SettingService.KEY_COMMUNITY_NAME, "阳光花园小区"),
                Map.entry(SettingService.KEY_SERVICE_PHONE, "13800000000"),
                Map.entry(SettingService.KEY_DELIVERY_SLOTS, "[\"07:00-09:00\",\"09:00-11:00\",\"11:00-13:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]"),
                Map.entry(SettingService.KEY_POINTS_RATE, "1"),
                Map.entry(SettingService.KEY_LEVEL_SILVER, "300"),
                Map.entry(SettingService.KEY_LEVEL_GOLD, "1000"),
                Map.entry(SettingService.KEY_LEVEL_BLACK, "3000"),
                Map.entry(SettingService.KEY_RESERVE_DEADLINE, "22:00")
        );
        defaults.forEach((k, v) -> {
            if (settingMapper.selectById(k) == null) {
                Setting s = new Setting();
                s.setKeyName(k);
                s.setValueText(v);
                s.setUpdatedAt(LocalDateTime.now());
                settingMapper.insert(s);
            }
        });
    }
}
