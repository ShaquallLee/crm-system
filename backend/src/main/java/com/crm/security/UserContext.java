package com.crm.security;

/**
 * 当前登录用户上下文（ThreadLocal）
 */
public class UserContext {

    public static final long ROLE_ADMIN = 1L;
    public static final long ROLE_MANAGER = 2L;
    public static final long ROLE_SALES = 3L;

    private static final ThreadLocal<LoginUser> CURRENT_USER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        CURRENT_USER.set(user);
    }

    public static LoginUser get() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }

    public static Long getUserId() {
        LoginUser user = get();
        return user != null ? user.getUserId() : null;
    }

    public static Long getRoleId() {
        LoginUser user = get();
        return user != null ? user.getRoleId() : null;
    }

    public static Long getManagerId() {
        LoginUser user = get();
        return user != null ? user.getManagerId() : null;
    }

    public static boolean isAdmin() {
        LoginUser user = get();
        return user != null && user.getRoleId() != null && user.getRoleId() == ROLE_ADMIN;
    }

    public static boolean isManager() {
        LoginUser user = get();
        return user != null && user.getRoleId() != null && user.getRoleId() == ROLE_MANAGER;
    }

    public static boolean isSales() {
        LoginUser user = get();
        return user != null && user.getRoleId() != null && user.getRoleId() == ROLE_SALES;
    }
}
