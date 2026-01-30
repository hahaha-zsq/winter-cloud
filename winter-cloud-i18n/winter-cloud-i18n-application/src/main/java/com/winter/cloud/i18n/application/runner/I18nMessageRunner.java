package com.winter.cloud.i18n.application.runner;

import cn.hutool.core.util.ObjectUtil;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.common.util.TtlExecutorUtils;
import com.winter.cloud.i18n.api.dto.response.I18nMessageDTO;
import com.winter.cloud.i18n.application.service.I18nMessageAppService;
import com.winter.cloud.i18n.application.service.impl.I18nMessageAppServiceImpl;
import com.winter.cloud.i18n.domain.repository.I18nMessageRepository;
import com.winter.cloud.i18n.infrastructure.repository.I18nMessageRepositoryImpl;
import com.zsq.i18n.service.I18nMessageService;
import com.zsq.winter.redis.ddc.service.WinterRedisTemplate;
import com.zsq.winter.redis.ddc.service.WinterRedissionTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * I18n 消息缓存预热启动器
 *
 * <p>该组件在应用启动完成后（依赖注入结束后）自动执行，负责将数据库中的国际化消息
 * 预热到 Redis 缓存和布隆过滤器中。</p>
 *
 * <h3>核心特性：</h3>
 * <ul>
 * <li><b>异步执行：</b>使用独立线程池，不阻塞主线程和应用启动速度。</li>
 * <li><b>上下文传递：</b>使用 TTL (TransmittableThreadLocal) 线程池，确保日志 TraceId 等上下文信息不丢失。</li>
 * <li><b>集群防并发：</b>使用 Redisson 分布式锁，防止多实例部署时所有节点同时预热（惊群效应）。</li>
 * <li><b>双重预热：</b>同时初始化布隆过滤器（防穿透）和 Redis 数据缓存（防冷启动击穿）。</li>
 * <li><b>防雪崩：</b>缓存过期时间添加随机因子。</li>
 * </ul>
 *
 * @author winter
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class I18nMessageRunner {

    private final WinterRedisTemplate redisTemplate;
    private final WinterRedissionTemplate redissionTemplate;
    private final I18nMessageRepositoryImpl i18nMessageRepository;
    /**
     * 支持 TTL 的自定义线程池，用于异步预热
     * <p>
     * 使用 {@link TtlExecutorUtils} 包装线程池，确保父线程的 ThreadLocal（如 TraceId、UserContext）
     * 能够正确传递到异步执行的子线程中，方便链路追踪和日志排查。
     * </p>
     */
    private final ExecutorService ttlExecutor = TtlExecutorUtils.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName(CommonConstants.I18nMessage.I18N_CACHE_WARMUP_THREAD_NAME); // 设置可读性强的线程名称
        thread.setDaemon(false); // 设置为用户线程（非守护线程），确保主线程退出前预热任务能执行完成
        return thread;
    });

    /**
     * 应用启动时的入口方法
     * <p>使用了 {@code @PostConstruct} 注解，在 Bean 初始化完成后立即执行。</p>
     */
    @PostConstruct
    public void start() {
        log.info("应用启动完成，开始异步执行 I18n 缓存预热...");

        /*
         * 使用 CompletableFuture 配合 TTL 线程池异步执行预热任务。
         * 目的：避免大量的数据库查询和 Redis 写入操作阻塞 Spring Boot 的主启动流程，加快服务就绪时间。
         */
        CompletableFuture.runAsync(() -> {
            // =========================================================================
            // 分布式锁控制 (Distributed Lock)
            // 目的：在微服务集群部署场景下，防止多个实例同时启动时重复执行预热任务，
            // 造成数据库压力瞬间飙升（惊群效应）以及 Redis 的重复写入浪费。
            // =========================================================================
            RLock lock = redissionTemplate.getLock(CommonConstants.I18nMessage.I18N_CACHE_WARMUP_LOCK_NAME);

            try {
                // 尝试获取锁：
                // waitTime = 0: 不等待。如果锁被别人持有，立即返回 false，说明已经有其他节点在预热了，本地直接跳过。
                // leaseTime = 3 min: 锁租期。如果当前节点预热过程中宕机，3分钟后锁自动释放，防止死锁。
                if (lock.tryLock(0, 3, TimeUnit.MINUTES)) {
                    try {
                        log.info("获取预热锁成功，当前实例负责执行 I18n 缓存预热...");
                        long startTime = System.currentTimeMillis();

                        // 1. 初始化布隆过滤器 (构建防穿透屏障)
                        i18nMessageRepository.initBloomFilter();

                        // 2. 预热 Redis 缓存 (构建热点数据)
                        i18nMessageRepository.warmupCache();

                        long endTime = System.currentTimeMillis();
                        log.info("I18n 缓存预热完成，耗时: {}ms", endTime - startTime);
                    } finally {
                        // 释放锁前必须判断当前线程是否持有该锁
                        // 防止任务执行时间过长导致锁自动过期后，释放了别人的锁
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                    }
                } else {
                    // 获取锁失败，说明其他实例正在执行或刚执行完，本地无需重复操作
                    log.info("检测到其他实例正在进行预热，本地跳过...");
                }
            } catch (Exception e) {
                // 捕获所有异常，确保预热失败不会导致应用崩溃，只记录错误日志
                log.error("I18n 缓存预热失败", e);
            }
        }, ttlExecutor).exceptionally(throwable -> {
            // 异步任务本身抛出的未捕获异常处理
            log.error("I18n 缓存预热异步任务执行失败", throwable);
            return null;
        });
    }

    /**
     * 应用销毁时的回调
     * <p>优雅关闭线程池，防止资源泄漏</p>
     */
    @PreDestroy
    public void destroy() {
        log.info("开始关闭 I18n 缓存预热线程池...");
        // 优雅关闭，等待正在执行的任务最多 30 秒
        TtlExecutorUtils.shutdownGracefully(ttlExecutor, 30);
    }
}