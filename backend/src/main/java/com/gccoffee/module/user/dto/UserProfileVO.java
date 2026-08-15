package com.gccoffee.module.user.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户会员信息（个人中心/会员页使用）
 */
@Data
public class UserProfileVO {

    private Long id;
    private String nickname;
    private String avatar;
    private String phone;
    private String role;
    private BigDecimal balance;
    private Integer points;
    private BigDecimal totalSpend;
    private String defaultAddress;
    private String levelName;
    /** 下一等级所需消费（null=已是最高等级） */
    private BigDecimal nextLevelNeed;

    public static UserProfileVO from(com.gccoffee.module.user.entity.User u, String levelName, BigDecimal nextLevelNeed) {
        UserProfileVO vo = new UserProfileVO();
        vo.id = u.getId();
        vo.nickname = u.getNickname();
        vo.avatar = u.getAvatar();
        vo.phone = u.getPhone();
        vo.role = u.getRole();
        vo.balance = u.getBalance();
        vo.points = u.getPoints();
        vo.totalSpend = u.getTotalSpend();
        vo.defaultAddress = u.getDefaultAddress();
        vo.levelName = levelName;
        vo.nextLevelNeed = nextLevelNeed;
        return vo;
    }
}
