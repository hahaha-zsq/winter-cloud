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

@Component
@Slf4j
@RequiredArgsConstructor
public class I18nMessageRunner {

    private final WinterRedisTemplate redisTemplate;
    private final WinterRedissionTemplate redissionTemplate;
    private final I18nMessageAppService i18nMessageAppService;
    private final Random random = new Random();

    /**
     * 支持 TTL 的线程池，用于异步预热
     * 确保 TraceId 等上下文信息能正确传递
     */
    private final ExecutorService ttlExecutor = TtlExecutorUtils.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName(CommonConstants.I18nMessage.I18N_CACHE_WARMUP_THREAD_NAME);
        thread.setDaemon(false); // 非守护线程，确保预热任务完成
        return thread;
    });

    @PostConstruct
    public void start() {
        log.info("应用启动完成，开始异步执行 I18n 缓存预热...");

        /*
         * 使用支持 TTL 的线程池异步执行预热任务，不阻塞应用启动
         * TTL 确保 TraceId 等上下文信息能正确传递
         */
        CompletableFuture.runAsync(() -> {
            // [新增] 添加分布式锁，防止多实例同时启动造成"惊群"效应
            RLock lock = redissionTemplate.getLock(CommonConstants.I18nMessage.I18N_CACHE_WARMUP_LOCK_NAME);

            try {
                // 尝试获取锁，不等待（tryLock 无参或带时间参数但等待时间为0），如果获取失败说明其他节点正在预热
                if (lock.tryLock(0, 5, TimeUnit.MINUTES)) {
                    try {
                        log.info("获取预热锁成功，开始执行 I18n 缓存预热...");
                        long startTime = System.currentTimeMillis();

                        // 1. 初始化布隆过滤器
                        initBloomFilter();

                        // 2. 预热缓存
                        warmupCache();

                        long endTime = System.currentTimeMillis();
                        log.info("I18n 缓存预热完成，耗时: {}ms", endTime - startTime);
                    } finally {
                        // 判断当前线程是否持有该锁，有就释放锁
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                    }
                } else {
                    log.info("检测到其他实例正在进行预热，本地跳过...");
                }
            } catch (Exception e) {
                log.error("I18n 缓存预热失败", e);
            }
        }, ttlExecutor).exceptionally(throwable -> {
            log.error("I18n 缓存预热异步任务执行失败", throwable);
            return null;
        });
    }

    @PreDestroy
    public void destroy() {
        log.info("开始关闭 I18n 缓存预热线程池...");
        TtlExecutorUtils.shutdownGracefully(ttlExecutor, 30);
    }

    public void warmupCache() {
        try {
            // 查询所有国际化消息
            // 注意：如果数据量过大（如超过10万条），建议此处改为分页查询分批处理，避免 OOM
            List<I18nMessageDTO> allMessages = i18nMessageAppService.getI18nMessageInfo(null);

            if (allMessages == null || allMessages.isEmpty()) {
                log.warn("没有找到需要预热的国际化消息");
                return;
            }

            int successCount = 0;
            for (I18nMessageDTO message : allMessages) {
                try {
                    String cacheKey = CommonConstants.buildI18nMessageKey(
                            message.getMessageKey(), message.getLocale());

                    // 添加随机过期时间，防止缓存雪崩
                    long expireSeconds = CommonConstants.I18nMessage.I18N_CACHE_EXPIRE_SECONDS
                                         + random.nextInt((int) CommonConstants.I18nMessage.I18N_CACHE_RANDOM_EXPIRE_SECONDS);

                    redisTemplate.set(
                            cacheKey,
                            message.getMessageValue(),
                            expireSeconds,
                            TimeUnit.SECONDS);

                    successCount++;
                } catch (Exception e) {
                    log.error("预热消息失败: key={}, locale={}",
                            message.getMessageKey(), message.getLocale(), e);
                }
            }

            log.info("缓存预热完成: 总数={}, 成功={}", allMessages.size(), successCount);
        } catch (Exception e) {
            log.error("缓存预热失败", e);
            throw new RuntimeException("缓存预热失败", e);
        }
    }

    public void initBloomFilter() {
        try {
            // [修改] 获取布隆过滤器实例
            RBloomFilter<Object> bloomFilter = redissionTemplate.getBloomFilter(CommonConstants.I18nMessage.I18N_BLOOM_FILTER_NAME);

            // [新增] 如果存在，先删除旧的过滤器，防止包含已删除数据的脏数据
            if (ObjectUtil.isNotEmpty(bloomFilter)) {
                log.info("检测到旧的布隆过滤器，正在删除以重建...");
                bloomFilter.delete();
            }

            // 创建并初始化
            bloomFilter.tryInit(CommonConstants.I18nMessage.I18N_BLOOM_EXPECTED_INSERTIONS,
                    CommonConstants.I18nMessage.I18N_BLOOM_FALSE_PROBABILITY);

            log.info("布隆过滤器初始化成功: name={}, expectedInsertions={}, falseProbability={}",
                    CommonConstants.I18nMessage.I18N_BLOOM_FILTER_NAME,
                    CommonConstants.I18nMessage.I18N_BLOOM_EXPECTED_INSERTIONS,
                    CommonConstants.I18nMessage.I18N_BLOOM_FALSE_PROBABILITY);

            // 查询所有消息键
            List<I18nMessageDTO> allMessages = i18nMessageAppService.getI18nMessageInfo(null);

            if (allMessages == null || allMessages.isEmpty()) {
                log.warn("没有找到需要加载到布隆过滤器的消息");
                return;
            }

            int addCount = 0;
            for (I18nMessageDTO message : allMessages) {
                try {
                    String bloomKey = CommonConstants.buildI18nBloomKey(
                            message.getMessageKey(), message.getLocale());
                    bloomFilter.add(bloomKey);
                    addCount++;
                } catch (Exception e) {
                    log.error("添加到布隆过滤器失败: key={}, locale={}",
                            message.getMessageKey(), message.getLocale(), e);
                }
            }

            log.info("布隆过滤器加载完成: 总数={}, 成功={}, 当前大小={}",
                    allMessages.size(), addCount, bloomFilter.count());
        } catch (Exception e) {
            log.error("初始化布隆过滤器失败", e);
            throw new RuntimeException("初始化布隆过滤器失败", e);
        }
    }
}
