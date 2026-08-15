package com.gccoffee.module.activity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gccoffee.common.Result;
import com.gccoffee.module.activity.entity.Activity;
import com.gccoffee.module.activity.entity.Poster;
import com.gccoffee.module.activity.mapper.ActivityMapper;
import com.gccoffee.module.activity.mapper.PosterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityMapper activityMapper;
    private final PosterMapper posterMapper;

    /** 活动列表：type=MONTHLY/QUARTERLY/ALL（进行中 + 即将开始） */
    @GetMapping("/list")
    public Result<List<Activity>> list(@RequestParam(defaultValue = "ALL") String type) {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<Activity> qw = new LambdaQueryWrapper<Activity>()
                .eq(Activity::getActive, 1)
                .ge(Activity::getEndAt, today);
        if (!"ALL".equals(type)) {
            qw.eq(Activity::getType, type);
        }
        qw.orderByAsc(Activity::getSort).orderByDesc(Activity::getId);
        return Result.ok(activityMapper.selectList(qw));
    }

    /** 海报列表（首页轮播用，公开接口也有） */
    @GetMapping("/posters")
    public Result<List<Poster>> posters() {
        return Result.ok(posterMapper.selectList(new LambdaQueryWrapper<Poster>()
                .eq(Poster::getActive, 1).orderByAsc(Poster::getSort).orderByDesc(Poster::getId)));
    }

    /** 活动详情（含已结束活动，海报跳转用） */
    @GetMapping("/detail")
    public Result<Activity> detail(@RequestParam Long id) {
        Activity a = activityMapper.selectById(id);
        if (a == null) {
            throw new com.gccoffee.common.BizException("活动不存在");
        }
        return Result.ok(a);
    }
}
