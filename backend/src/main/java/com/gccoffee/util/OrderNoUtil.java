package com.gccoffee.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public final class OrderNoUtil {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private OrderNoUtil() {
    }

    private static String random4() {
        return String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    public static String drink() {
        return "D" + LocalDateTime.now().format(FMT) + random4();
    }

    public static String coffee() {
        return "R" + LocalDateTime.now().format(FMT) + random4();
    }

    public static String packageNo() {
        return "P" + LocalDateTime.now().format(FMT) + random4();
    }

    public static String recharge() {
        return "RC" + LocalDateTime.now().format(FMT) + random4();
    }
}
