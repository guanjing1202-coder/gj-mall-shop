package com.gj.mall.framework.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前请求用户上下文（基于 ThreadLocal）
 */
public class UserContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    public static void set(CurrentUser user) { HOLDER.set(user); }

    public static CurrentUser get() { return HOLDER.get(); }

    public static Long getUserId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.getUserId();
    }

    public static String getUserType() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.getUserType();
    }

    public static void clear() { HOLDER.remove(); }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentUser {
        private Long userId;
        /** "user" | "admin" */
        private String userType;
        private String username;
    }
}
