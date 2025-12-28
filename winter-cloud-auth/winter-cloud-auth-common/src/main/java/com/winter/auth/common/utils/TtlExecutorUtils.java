package com.winter.auth.common.utils;

import com.alibaba.ttl.TtlCallable;
import com.alibaba.ttl.TtlRunnable;
import com.alibaba.ttl.threadpool.TtlExecutors;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * TTL线程池包装工具类
 * 提供统一的线程池管理，确保TransmittableThreadLocal在多线程环境下正确传递
 * 
 * @author zsq
 */
@Slf4j
public class TtlExecutorUtils {
    
    /**
     * 包装ExecutorService，使其支持TTL
     * 
     * @param executor 原始线程池
     * @return 支持TTL的线程池
     */
    public static ExecutorService wrapExecutorService(ExecutorService executor) {
        if (executor == null) {
            throw new IllegalArgumentException("ExecutorService不能为null");
        }
        
        ExecutorService ttlExecutor = TtlExecutors.getTtlExecutorService(executor);
        log.debug("包装ExecutorService为TTL支持的线程池");
        return ttlExecutor;
    }
    
    /**
     * 包装ScheduledExecutorService，使其支持TTL
     * 
     * @param executor 原始定时线程池
     * @return 支持TTL的定时线程池
     */
    public static ScheduledExecutorService wrapScheduledExecutorService(ScheduledExecutorService executor) {
        if (executor == null) {
            throw new IllegalArgumentException("ScheduledExecutorService不能为null");
        }
        
        ScheduledExecutorService ttlExecutor = TtlExecutors.getTtlScheduledExecutorService(executor);
        log.debug("包装ScheduledExecutorService为TTL支持的定时线程池");
        return ttlExecutor;
    }
    
    /**
     * 包装Executor，使其支持TTL
     * 
     * @param executor 原始执行器
     * @return 支持TTL的执行器
     */
    public static Executor wrapExecutor(Executor executor) {
        if (executor == null) {
            throw new IllegalArgumentException("Executor不能为null");
        }
        
        Executor ttlExecutor = TtlExecutors.getTtlExecutor(executor);
        log.debug("包装Executor为TTL支持的执行器");
        return ttlExecutor;
    }
    
    /**
     * 包装Runnable任务，使其支持TTL
     * 
     * @param runnable 原始任务
     * @return 支持TTL的任务
     */
    public static Runnable wrapRunnable(Runnable runnable) {
        if (runnable == null) {
            return null;
        }
        
        return TtlRunnable.get(runnable);
    }
    
    /**
     * 包装Callable任务，使其支持TTL
     * 
     * @param callable 原始任务
     * @param <T> 返回值类型
     * @return 支持TTL的任务
     */
    public static <T> Callable<T> wrapCallable(Callable<T> callable) {
        if (callable == null) {
            return null;
        }
        
        return TtlCallable.get(callable);
    }
    
    /**
     * 创建支持TTL的固定大小线程池
     * 
     * @param nThreads 线程数量
     * @return 支持TTL的线程池
     */
    public static ExecutorService newFixedThreadPool(int nThreads) {
        return wrapExecutorService(Executors.newFixedThreadPool(nThreads));
    }
    
    /**
     * 创建支持TTL的固定大小线程池（带线程工厂）
     * 
     * @param nThreads 线程数量
     * @param threadFactory 线程工厂
     * @return 支持TTL的线程池
     */
    public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
        return wrapExecutorService(Executors.newFixedThreadPool(nThreads, threadFactory));
    }
    
    /**
     * 创建支持TTL的缓存线程池
     * 
     * @return 支持TTL的线程池
     */
    public static ExecutorService newCachedThreadPool() {
        return wrapExecutorService(Executors.newCachedThreadPool());
    }
    
    /**
     * 创建支持TTL的缓存线程池（带线程工厂）
     * 
     * @param threadFactory 线程工厂
     * @return 支持TTL的线程池
     */
    public static ExecutorService newCachedThreadPool(ThreadFactory threadFactory) {
        return wrapExecutorService(Executors.newCachedThreadPool(threadFactory));
    }
    
    /**
     * 创建支持TTL的单线程池
     * 
     * @return 支持TTL的线程池
     */
    public static ExecutorService newSingleThreadExecutor() {
        return wrapExecutorService(Executors.newSingleThreadExecutor());
    }
    
    /**
     * 创建支持TTL的单线程池（带线程工厂）
     * 
     * @param threadFactory 线程工厂
     * @return 支持TTL的线程池
     */
    public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
        return wrapExecutorService(Executors.newSingleThreadExecutor(threadFactory));
    }
    
    /**
     * 创建支持TTL的定时线程池
     * 
     * @param corePoolSize 核心线程数
     * @return 支持TTL的定时线程池
     */
    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) {
        return wrapScheduledExecutorService(Executors.newScheduledThreadPool(corePoolSize));
    }
    
    /**
     * 创建支持TTL的定时线程池（带线程工厂）
     * 
     * @param corePoolSize 核心线程数
     * @param threadFactory 线程工厂
     * @return 支持TTL的定时线程池
     */
    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize, ThreadFactory threadFactory) {
        return wrapScheduledExecutorService(Executors.newScheduledThreadPool(corePoolSize, threadFactory));
    }
    
    /**
     * 创建支持TTL的单线程定时池
     * 
     * @return 支持TTL的定时线程池
     */
    public static ScheduledExecutorService newSingleThreadScheduledExecutor() {
        return wrapScheduledExecutorService(Executors.newSingleThreadScheduledExecutor());
    }
    
    /**
     * 创建支持TTL的单线程定时池（带线程工厂）
     * 
     * @param threadFactory 线程工厂
     * @return 支持TTL的定时线程池
     */
    public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory threadFactory) {
        return wrapScheduledExecutorService(Executors.newSingleThreadScheduledExecutor(threadFactory));
    }
    
    /**
     * 安全关闭线程池
     * 
     * @param executor 线程池
     * @param timeoutSeconds 超时时间（秒）
     */
    public static void shutdownGracefully(ExecutorService executor, long timeoutSeconds) {
        if (executor == null || executor.isShutdown()) {
            return;
        }
        
        try {
            // 停止接收新任务
            executor.shutdown();
            
            // 等待现有任务完成
            if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                log.warn("线程池在{}秒内未能正常关闭，强制关闭", timeoutSeconds);
                executor.shutdownNow();
                
                // 再次等待
                if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                    log.error("线程池强制关闭失败");
                }
            }
            
            log.info("线程池已安全关闭");
        } catch (InterruptedException e) {
            log.error("线程池关闭过程中被中断", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}