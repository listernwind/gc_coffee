package com.gccoffee.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public final class OrderNoUtil {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private OrderNoUtil() {
    }

    /** 毫秒级时间戳 + 3 位随机数，降低撞号概率 */
    private static String random3() {
        return String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }

    public static String drink() {
        return "D" + LocalDateTime.now().format(FMT) + random3();
    }

    public static String coffee() {
        return "R" + LocalDateTime.now().format(FMT) + random3();
    }

    public static String packageNo() {
        return "P" + LocalDateTime.now().format(FMT) + random3();
    }

    public static String recharge() {
        return "RC" + LocalDateTime.now().format(FMT) + random3();
    }
}
