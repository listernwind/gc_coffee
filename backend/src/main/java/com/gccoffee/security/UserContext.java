package com.gccoffee.security;

/**
 * 当前请求用户上下文（由 AuthInterceptor 填充）
 */
public class UserContext {

    private static final ThreadLocal<Long> UID = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();

    public static void set(Long uid, String role) {
        UID.set(uid);
        ROLE.set(role);
    }

    public static Long uid() {
        Long uid = UID.get();
        if (uid == null) {
            throw new com.gccoffee.common.BizException(401, "请先登录");
        }
        return uid;
    }

    public static String role() {
        return ROLE.get();
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(ROLE.get());
    }

    public static void clear() {
        UID.remove();
        ROLE.remove();
    }
}
