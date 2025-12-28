package com.winter.auth.common.utils;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 登录用户信息工具类
 * 使用 TransmittableThreadLocal 支持异步场景
 */
public class LoginUtil {

    /**
     * 使用 TTL 存储当前登录用户ID
     */
    private static final TransmittableThreadLocal<Long> USER_ID_HOLDER = new TransmittableThreadLocal<>();

    /**
     * 设置当前登录用户ID
     */
    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * 获取当前登录用户ID
     */
    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 清除当前登录用户ID
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
