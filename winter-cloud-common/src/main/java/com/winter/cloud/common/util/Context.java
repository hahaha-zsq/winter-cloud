package com.winter.cloud.common.util;

import cn.hutool.core.util.IdUtil;
import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 上下文工具类
 * 使用阿里的 TransmittableThreadLocal (TTL) 实现线程间上下文传递
 */
public class Context {

    /**
     * 链路追踪ID TransmittableThreadLocal
     * TTL 可以在线程池、异步调用等场景下自动传递上下文
     */
    private static final TransmittableThreadLocal<String> TRACE_ID_HOLDER = new TransmittableThreadLocal<>();

    /**
     * 获取链路追踪ID
     * 如果当前线程没有 traceId，则自动生成一个
     *
     * @return 链路追踪ID
     */
    public static String getTraceId() {
        String traceId = TRACE_ID_HOLDER.get();
        if (traceId == null) {
            traceId = initTraceId();
        }
        return traceId;
    }

    /**
     * 初始化链路追踪ID
     * 生成新的 traceId 并设置到当前线程
     *
     * @return 新生成的链路追踪ID
     */
    public static String initTraceId() {
        String traceId = IdUtil.fastSimpleUUID();
        TRACE_ID_HOLDER.set(traceId);
        return traceId;
    }

    /**
     * 设置链路追踪ID
     *
     * @param traceId 链路追踪ID
     */
    public static void setTraceId(String traceId) {
        TRACE_ID_HOLDER.set(traceId);
    }

    /**
     * 清除链路追踪ID
     */
    public static void clearTraceId() {
        TRACE_ID_HOLDER.remove();
    }
}
