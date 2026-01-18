package com.winter.cloud.i18n.interfaces.interceptor;

import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.common.util.Context;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * TraceId 拦截器
 * <p>
 * 负责在请求进入时初始化 traceId，在请求结束时清理 traceId。
 * 这是 DDD 架构中接口层（interfaces）的职责，确保每个请求都有唯一的追踪标识。
 * </p>
 * 
 * @author zsq
 */
@Slf4j
@Component
public class TraceIdInterceptor implements HandlerInterceptor {


    /**
     * 在请求处理之前执行
     * <p>
     * 优先从请求头获取 traceId（支持分布式追踪），
     * 如果请求头中没有，则自动生成新的 traceId。
     * </p>
     */
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                            @NonNull HttpServletResponse response,
                            @NonNull Object handler) {
        // 尝试从请求头获取 traceId
        String traceId = request.getHeader(CommonConstants.Headers.TRACE_ID);
        
        if (traceId == null || traceId.trim().isEmpty()) {
            // 如果请求头中没有 traceId，则生成新的
            traceId = Context.initTraceId();
            log.debug("生成新的 traceId: {}", traceId);
        } else {
            // 使用请求头中的 traceId
            Context.setTraceId(traceId);
            log.debug("使用请求头中的 traceId: {}", traceId);
        }
        
        // 将 traceId 设置到响应头中，方便客户端追踪
        response.setHeader(CommonConstants.Headers.TRACE_ID, traceId);
        
        return true;
    }

    /**
     * 在请求完成后执行
     * <p>
     * 清理 ThreadLocal 中的 traceId，防止内存泄漏。
     * 这在使用线程池的场景下尤为重要。
     * </p>
     */
    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, 
                               @NonNull HttpServletResponse response, 
                               @NonNull Object handler, 
                               Exception ex) {
        // 清理 ThreadLocal，防止内存泄漏
        Context.clearTraceId();
        log.debug("清理 traceId");
    }
}
