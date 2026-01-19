package com.winter.cloud.i18n.application.runner;

import cn.hutool.core.util.ObjectUtil;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.common.util.TtlExecutorUtils;
import com.winter.cloud.i18n.api.dto.response.I18nMessageDTO;
import com.winter.cloud.i18n.application.service.I18nMessageAppService;
import com.zsq.winter.redis.ddc.service.WinterRedisTemplate;
import com.zsq.winter.redis.ddc.service.WinterRedissionTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
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
    private final I18nMessageAppService i18nMessageAppService;
    private final Random random = new Random();

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
                        initBloomFilter();

                        // 2. 预热 Redis 缓存 (构建热点数据)
                        warmupCache();

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

    /**
     * 核心逻辑：将数据库数据加载到 Redis
     */
    public void warmupCache() {
        try {
            // 1. 查询所有国际化消息
            // 注意：此处是全量查询。如果未来数据量突破 10w+，建议改为分页分批查询（MyBatis Cursor 或 PageHelper），
            // 避免一次性加载过多对象导致 JVM OOM。
            List<I18nMessageDTO> allMessages = i18nMessageAppService.getI18nMessageInfo(null);

            if (allMessages == null || allMessages.isEmpty()) {
                log.warn("没有找到需要预热的国际化消息");
                return;
            }

            int successCount = 0;
            // 2. 遍历并写入 Redis
            for (I18nMessageDTO message : allMessages) {
                try {
                    // 构建缓存 Key: "i18n:message:{key}:{locale}"
                    String cacheKey = CommonConstants.buildI18nMessageKey(
                            message.getMessageKey(), message.getLocale());

                    // =====================================================================
                    // 防雪崩策略 (Cache Avalanche Prevention)
                    // 如果所有缓存在同一时刻（如重启后1小时）集体失效，会导致请求瞬间击穿到 DB。
                    // 解决方案：expire = 基础时间 (e.g. 24h) + 随机时间 (e.g. 0-60min)
                    // =====================================================================
                    long expireSeconds = CommonConstants.I18nMessage.I18N_CACHE_EXPIRE_SECONDS
                                         + random.nextInt((int) CommonConstants.I18nMessage.I18N_CACHE_RANDOM_EXPIRE_SECONDS);

                    // 写入 Redis
                    redisTemplate.set(
                            cacheKey,
                            message.getMessageValue(),
                            expireSeconds,
                            TimeUnit.SECONDS);

                    successCount++;
                } catch (Exception e) {
                    // 单条数据失败不影响整体
                    log.error("预热消息失败: key={}, locale={}",
                            message.getMessageKey(), message.getLocale(), e);
                }
            }

            log.info("Redis 缓存预热完成: 总数={}, 成功写入={}", allMessages.size(), successCount);
        } catch (Exception e) {
            log.error("Redis 缓存预热整体失败", e);
            throw new RuntimeException("缓存预热失败", e);
        }
    }

    /**
     * 核心逻辑：初始化布隆过滤器
     * <p>布隆过滤器用于快速判断一个 Key 是否<b>肯定不存在</b>，从而拦截无效请求访问 DB。</p>
     */
    public void initBloomFilter() {
        try {
            // 1. 获取 Redisson 的布隆过滤器实例
            RBloomFilter<Object> bloomFilter = redissionTemplate.getBloomFilter(CommonConstants.I18nMessage.I18N_BLOOM_FILTER_NAME);

            // =========================================================================
            // 重建策略
            // 布隆过滤器不支持删除单个元素。如果数据库中删除了数据，布隆过滤器里还有，会造成误判。
            // 因此，每次全量预热时，最佳实践是删除旧的过滤器，根据当前 DB 数据重新构建。
            // =========================================================================
            if (ObjectUtil.isNotEmpty(bloomFilter)) {
                log.info("检测到旧的布隆过滤器，正在删除以重建...");
                bloomFilter.delete();
            }

            // 2. 初始化配置
            // expectedInsertions: 预计插入元素数量（根据业务预估，设置大一点防止误判率升高）
            // falseProbability: 期望的误判率（通常 0.01 或 0.03，越低占用内存越大）
            bloomFilter.tryInit(CommonConstants.I18nMessage.I18N_BLOOM_EXPECTED_INSERTIONS,
                    CommonConstants.I18nMessage.I18N_BLOOM_FALSE_PROBABILITY);

            log.info("布隆过滤器初始化成功: name={}, 预计容量={}, 误判率={}",
                    CommonConstants.I18nMessage.I18N_BLOOM_FILTER_NAME,
                    CommonConstants.I18nMessage.I18N_BLOOM_EXPECTED_INSERTIONS,
                    CommonConstants.I18nMessage.I18N_BLOOM_FALSE_PROBABILITY);

            // 3. 获取全量数据准备加载
            List<I18nMessageDTO> allMessages = i18nMessageAppService.getI18nMessageInfo(null);

            if (ObjectUtil.isEmpty(allMessages)) {
                log.warn("没有找到需要加载到布隆过滤器的消息");
                return;
            }

            int addCount = 0;
            // 4. 将所有存在的 Key + Locale 组合写入布隆过滤器
            for (I18nMessageDTO message : allMessages) {
                try {
                    // 构建布隆 Key (需与查询时构建逻辑一致)
                    String bloomKey = CommonConstants.buildI18nBloomKey(
                            message.getMessageKey(), message.getLocale());
                    bloomFilter.add(bloomKey);
                    addCount++;
                } catch (Exception e) {
                    log.error("添加到布隆过滤器失败: key={}, locale={}",
                            message.getMessageKey(), message.getLocale(), e);
                }
            }

            log.info("布隆过滤器加载完成: 数据库总数={}, 成功加载={}, 过滤器当前计数={}",
                    allMessages.size(), addCount, bloomFilter.count());
        } catch (Exception e) {
            log.error("初始化布隆过滤器失败", e);
            throw new RuntimeException("初始化布隆过滤器失败", e);
        }
    }
}