package com.fullstack.demo.shared.auth;

/**
 * 当前请求用户上下文（ThreadLocal）。
 * <p>
 * 由 JwtAuthFilter 写入并在 finally 中清理；application 层服务只读。
 */
public final class UserContextHolder {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
