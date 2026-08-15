package com.gccoffee.module.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gccoffee.common.Result;
import com.gccoffee.module.admin.entity.Setting;
import com.gccoffee.module.admin.service.SettingService;
import com.gccoffee.module.activity.entity.Poster;
import com.gccoffee.module.activity.mapper.PosterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 无需登录的公开接口（首页基础数据、配送时段等）
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final SettingService settingService;
    private final PosterMapper posterMapper;

    /** 首页数据：门店信息 + 公告 + 海报轮播 */
    @GetMapping("/home")
    public Result<Map<String, Object>> home() {
        Map<String, String> settings = settingService.getAll();
        List<Poster> posters = posterMapper.selectList(new LambdaQueryWrapper<Poster>()
                .eq(Poster::getActive, 1).orderByAsc(Poster::getSort).orderByDesc(Poster::getId));
        return Result.ok(Map.of(
                "settings", settings,
                "posters", posters
        ));
    }

    /** 全部公开设置（小程序端用到配送时段、小区名等） */
    @GetMapping("/settings")
    public Result<Map<String, String>> settings() {
        return Result.ok(settingService.getAll());
    }

    @GetMapping("/health")
    public Result<String> health() {
        return Result.ok("up");
    }
}
