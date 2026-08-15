package com.gccoffee.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gccoffee.module.admin.entity.Setting;
import com.gccoffee.module.admin.mapper.SettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统设置（键值对）。默认值见 DataInitializer。
 */
@Service
@RequiredArgsConstructor
public class SettingService {

    public static final String KEY_SHOP_NAME = "shop_name";
    public static final String KEY_SHOP_NOTICE = "shop_notice";
    public static final String KEY_COMMUNITY_NAME = "community_name";
    public static final String KEY_SERVICE_PHONE = "service_phone";
    public static final String KEY_DELIVERY_SLOTS = "delivery_slots";
    public static final String KEY_POINTS_RATE = "points_rate";
    public static final String KEY_LEVEL_SILVER = "level_silver";
    public static final String KEY_LEVEL_GOLD = "level_gold";
    public static final String KEY_LEVEL_BLACK = "level_black";
    public static final String KEY_RESERVE_DEADLINE = "reserve_deadline";
    public static final String KEY_DELIVERY_FEE = "delivery_fee";

    private final SettingMapper settingMapper;
    private final ObjectMapper objectMapper;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String get(String key, String def) {
        String v = cache.get(key);
        if (v != null) {
            return v;
        }
        Setting s = settingMapper.selectById(key);
        v = s == null ? def : s.getValueText();
        cache.put(key, v);
        return v;
    }

    public int getInt(String key, int def) {
        try {
            return Integer.parseInt(get(key, String.valueOf(def)));
        } catch (Exception e) {
            return def;
        }
    }

    public BigDecimal getDecimal(String key, BigDecimal def) {
        try {
            return new BigDecimal(get(key, def.toPlainString()));
        } catch (Exception e) {
            return def;
        }
    }

    public List<String> getDeliverySlots() {
        String json = get(KEY_DELIVERY_SLOTS, "[]");
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    public Map<String, String> getAll() {
        List<Setting> list = settingMapper.selectList(new LambdaQueryWrapper<>());
        Map<String, String> map = new ConcurrentHashMap<>();
        for (Setting s : list) {
            map.put(s.getKeyName(), s.getValueText());
        }
        return map;
    }

    public void put(String key, String value) {
        Setting s = settingMapper.selectById(key);
        if (s == null) {
            s = new Setting();
            s.setKeyName(key);
            s.setValueText(value);
            settingMapper.insert(s);
        } else {
            s.setValueText(value);
            settingMapper.updateById(s);
        }
        cache.put(key, value);
    }

    public void putAll(Map<String, String> map) {
        map.forEach(this::put);
    }
}
