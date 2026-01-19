package com.winter.cloud.i18n.infrastructure.repository;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.i18n.api.dto.query.I18nMessageQuery;
import com.winter.cloud.i18n.domain.model.entity.I18nMessageDO;
import com.winter.cloud.i18n.domain.repository.I18nMessageRepository;
import com.winter.cloud.i18n.infrastructure.assembler.I18nMessageInfraAssembler;
import com.winter.cloud.i18n.infrastructure.entity.I18nMessagePO;
import com.winter.cloud.i18n.infrastructure.mapper.I18nMessageMapper;
import com.zsq.winter.redis.ddc.service.WinterRedisTemplate;
import com.zsq.winter.redis.ddc.service.WinterRedissionTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.springframework.stereotype.Repository;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 国际化消息仓储实现
 *
 * <p>实现 Winter I18n 框架的 I18nMessageRepository 接口,提供国际化消息的获取和查询功能。
 * 使用独立的 i18n 数据源,与主业务数据库隔离。
 * @see I18nMessageRepository
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class I18nMessageRepositoryImpl implements I18nMessageRepository {

    private final I18nMessageMapper messageMapper;
    private final WinterRedisTemplate winterRedisTemplate;
    private final WinterRedissionTemplate winterRedissionTemplate;
    private final I18nMessageInfraAssembler i18nMessageInfraAssembler;
    private final Random random = new Random();

    // ==================== Winter I18n 框架接口实现 ====================

    @Override
    public String getMessage(String messageKey, Locale locale) {
        return getMessage(messageKey, null, null, locale);
    }

    @Override
    public String getMessage(String messageKey, Object[] args, Locale locale) {
        return getMessage(messageKey, args, null, locale);
    }

    @Override
    public String getMessage(String messageKey, Object[] args, String defaultMessage, Locale locale) {
        String localeStr = locale.toString();
        String cacheKey = CommonConstants.buildI18nMessageKey(messageKey, localeStr);

        try {
            // 1. 先从 Redis 缓存获取
            String cachedMessage = (String) winterRedisTemplate.get(cacheKey);
            // 1.1 缓存命中
            if (cachedMessage != null) {
                // 如果是空值缓存，返回默认值（防止缓存穿透）
                if (CommonConstants.I18nMessage.I18N_NULL_VALUE.equals(cachedMessage)) {
                    log.debug("命中空值缓存: key={}, locale={}", messageKey, localeStr);
                    return getDefaultMessage(messageKey, args, defaultMessage, localeStr);
                }
                log.debug("从缓存获取消息: key={}, locale={}", messageKey, localeStr);
                return formatMessage(cachedMessage, args);
            }

            // 2. 缓存未命中，使用互斥锁防止缓存击穿
            String message = getMessageWithLock(messageKey, localeStr, cacheKey);

            if (message != null) {
                return formatMessage(message, args);
            }

            // 3. 尝试获取默认语言的消息
            if (!CommonConstants.I18nMessage.DEFAULT_LOCALE.equals(localeStr)) {
                String defaultMessage2 = getDefaultLocaleMessage(messageKey, args);
                if (defaultMessage2 != null) {
                    return defaultMessage2;
                }
            }

        } catch (Exception e) {
            log.error("获取国际化消息失败: key={}, locale={}", messageKey, localeStr, e);
        }

        // 4. 返回默认消息或消息键
        return defaultMessage != null ? defaultMessage : messageKey;
    }

    /**
     * 使用布隆过滤器和分布式锁获取消息
     * ... (保留原有 Javadoc) ...
     */
    private String getMessageWithLock(String messageKey, String locale, String cacheKey) {
        // 1. 先通过布隆过滤器判断数据是否可能存在（防止缓存穿透）
        String bloomKey = CommonConstants.buildI18nBloomKey(messageKey, locale);
        RBloomFilter<Object> bloomFilter = winterRedissionTemplate.getBloomFilter(CommonConstants.I18nMessage.I18N_BLOOM_FILTER_NAME);

        // 元素不存在的情况
        if (!bloomFilter.contains(bloomKey)) {
            log.debug("布隆过滤器判断数据不存在: key={}, locale={}", messageKey, locale);
            // 缓存空值，防止频繁查询
            winterRedisTemplate.set(cacheKey, CommonConstants.I18nMessage.I18N_NULL_VALUE, CommonConstants.I18nMessage.I18N_NULL_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
            return null;
        }

        // 2. 元素可能存在的情况（布隆过滤器存在一定的误判率），使用 Redisson 分布式锁（防止缓存击穿）
        String lockKey = CommonConstants.buildI18nLockKey(messageKey, locale);
        // 获取锁
        RLock lock = winterRedissionTemplate.getLock(lockKey);

        try {
            // 尝试获取锁，最多等待 3 秒，锁自动过期时间 10 秒
            if (lock.tryLock(3, CommonConstants.I18nMessage.I18N_LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS)) {
                try {
                    // 双重检查：获取锁后再次检查缓存
                    String cachedMessage = (String) winterRedisTemplate.get(cacheKey);
                    if (cachedMessage != null) {
                        // 存在值且不等于字符串null,返回缓存值，否则返回null
                        if (!CommonConstants.I18nMessage.I18N_NULL_VALUE.equals(cachedMessage)) {
                            return cachedMessage;
                        }
                        return null;
                    }

                    // 缓存不存在，查询数据库
                    log.debug("获取锁成功，查询数据库: key={}, locale={}", messageKey, locale);
                    String message = findMessageByKeyAndLocale(messageKey, locale);
                    // 数据库查出来的数据不为null的话，即数据库中存在数据
                    if (message != null) {
                        // 缓存到 Redis，添加随机过期时间（防止缓存雪崩）
                        long expireSeconds = CommonConstants.I18nMessage.I18N_CACHE_EXPIRE_SECONDS
                                             + random.nextInt((int) CommonConstants.I18nMessage.I18N_CACHE_RANDOM_EXPIRE_SECONDS);
                        // 添加缓存
                        winterRedisTemplate.set(cacheKey, message, expireSeconds, TimeUnit.SECONDS);
                        log.debug("从数据库获取消息并缓存: key={}, locale={}, expire={}s", messageKey, locale, expireSeconds);
                        return message;
                    } else {    // 数据库查出来的数据为null的话，缓存空字符串null，并返回null
                        // 缓存空值（防止缓存穿透），
                        winterRedisTemplate.set(cacheKey, CommonConstants.I18nMessage.I18N_NULL_VALUE,
                                CommonConstants.I18nMessage.I18N_NULL_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
                        log.debug("缓存空值: key={}, locale={}", messageKey, locale);
                        return null;
                    }
                } finally {
                    lock.unlock();
                }
            } else {
                // [修改] 获取锁超时，不直接放弃，尝试自旋获取缓存
                // 场景：持有锁的线程正在查询数据库，马上就会回写缓存，稍等一下能拿到结果的概率很高
                log.debug("获取锁超时，进入自旋等待: key={}, locale={}", messageKey, locale);

                // 自旋 3 次，每次等待 50ms (总计等待 150ms)
                for (int i = 0; i < 3; i++) {
                    Thread.sleep(50);
                    String cachedMessage = (String) winterRedisTemplate.get(cacheKey);
                    if (cachedMessage != null && !CommonConstants.I18nMessage.I18N_NULL_VALUE.equals(cachedMessage)) {
                        log.debug("自旋等待后命中缓存: key={}, locale={}", messageKey, locale);
                        return cachedMessage;
                    }
                }
                log.warn("自旋等待后仍未获取到消息，返回 null: key={}, locale={}", messageKey, locale);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取消息时被中断: key={}, locale={}", messageKey, locale, e);
        } catch (Exception e) {
            log.error("获取消息失败: key={}, locale={}", messageKey, locale, e);
        }

        return null;
    }

    private String getDefaultLocaleMessage(String messageKey, Object[] args) {
        String defaultCacheKey = CommonConstants.buildI18nMessageKey(messageKey, CommonConstants.I18nMessage.DEFAULT_LOCALE);
        String defaultCachedMessage = (String) winterRedisTemplate.get(defaultCacheKey);

        if (defaultCachedMessage != null) {
            if (!CommonConstants.I18nMessage.I18N_NULL_VALUE.equals(defaultCachedMessage)) {
                log.debug("使用默认语言缓存消息: key={}, locale={}", messageKey, CommonConstants.I18nMessage.DEFAULT_LOCALE);
                return formatMessage(defaultCachedMessage, args);
            }
            return null;
        }

        String defaultMessage = getMessageWithLock(messageKey, CommonConstants.I18nMessage.DEFAULT_LOCALE, defaultCacheKey);
        if (defaultMessage != null) {
            log.debug("使用默认语言数据库消息: key={}, locale={}", messageKey, CommonConstants.I18nMessage.DEFAULT_LOCALE);
            return formatMessage(defaultMessage, args);
        }

        return null;
    }

    private String getDefaultMessage(String messageKey, Object[] args, String defaultMessage, String locale) {
        if (!CommonConstants.I18nMessage.DEFAULT_LOCALE.equals(locale)) {
            String defaultLocaleMessage = getDefaultLocaleMessage(messageKey, args);
            if (defaultLocaleMessage != null) {
                return defaultLocaleMessage;
            }
        }
        return defaultMessage != null ? defaultMessage : messageKey;
    }

    private String formatMessage(String message, Object[] args) {
        if (args != null && args.length > 0) {
            try {
                return MessageFormat.format(message, args);
            } catch (Exception e) {
                log.warn("消息格式化失败: message={}, args={}", message, Arrays.toString(args), e);
                return message;
            }
        }
        return message;
    }

    /**
     * 根据消息键和语言环境查询消息内容
     * <p>直接查询数据库,不经过缓存
     * * @apiNote 注意：此方法不走缓存，仅用于缓存未命中时的回源查询，请勿在高频业务中直接调用
     */
    @Override
    public String findMessageByKeyAndLocale(String messageKey, String locale) {
        LambdaQueryWrapper<I18nMessagePO> queryWrapper = new LambdaQueryWrapper<I18nMessagePO>()
                .eq(I18nMessagePO::getMessageKey, messageKey)
                .eq(I18nMessagePO::getLocale, locale);
        I18nMessagePO i18nMessagePO = messageMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNotEmpty(i18nMessagePO)) {
            return i18nMessagePO.getMessageValue();
        }
        return null;
    }

    @Override
    public List<I18nMessageDO> getI18nMessageInfo(I18nMessageQuery query) {
        List<I18nMessagePO> i18nMessageInfo = messageMapper.getI18nMessageInfo(query);
        return i18nMessageInfraAssembler.toDOList(i18nMessageInfo);
    }
}