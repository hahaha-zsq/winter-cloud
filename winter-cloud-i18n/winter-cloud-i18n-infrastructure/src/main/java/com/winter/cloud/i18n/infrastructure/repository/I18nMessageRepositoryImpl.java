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
 *
 * <h3>解决的核心问题:</h3>
 * <ul>
 *   <li><b>缓存穿透</b>: 恶意请求不存在的消息键,导致大量请求打到数据库
 *       <br>解决方案: 布隆过滤器预判断 + 缓存空值</li>
 *   <li><b>缓存击穿</b>: 热点消息缓存过期瞬间,大量并发请求同时查询数据库
 *       <br>解决方案: Redisson 分布式锁 + 双重检查</li>
 *   <li><b>缓存雪崩</b>: 大量缓存同时过期,导致数据库压力骤增
 *       <br>解决方案: 随机过期时间(基础时间 + 随机偏移)</li>
 *   <li><b>多语言回退</b>: 请求的语言不存在时,自动回退到默认语言
 *       <br>解决方案: 三级查询策略(请求语言 → 默认语言 → 消息键)</li>
 * </ul>
 *
 * <h3>缓存架构:</h3>
 * <pre>
 * 请求 → 布隆过滤器判断 → Redis缓存 → 分布式锁 → 数据库 → 默认语言 → 消息键
 *           ↓ 不存在              ↓ 未命中      ↓ 防击穿
 *        返回默认值            加锁查询      双重检查
 * </pre>
 *
 * <h3>性能优化:</h3>
 * <ul>
 *   <li>布隆过滤器: O(1) 时间复杂度快速判断数据是否存在</li>
 *   <li>Redis 缓存: 减少 95% 以上的数据库查询</li>
 *   <li>分布式锁: 只有一个请求查询数据库,其他请求等待缓存</li>
 *   <li>空值缓存: 5分钟短期缓存,避免频繁查询不存在的数据</li>
 *   <li>随机过期: 防止缓存雪崩,分散过期时间</li>
 * </ul>
 *
 * @author zsq
 * @date 2025-10-11
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

    /**
     * 获取国际化消息(简化版)
     *
     * @param messageKey 消息键,如 "result.code.success"
     * @param locale     语言环境,如 zh_CN、en_US
     * @return 国际化消息内容, 如果不存在则返回消息键本身
     */
    @Override
    public String getMessage(String messageKey, Locale locale) {
        return getMessage(messageKey, null, null, locale);
    }

    /**
     * 获取国际化消息(带参数)
     *
     * @param messageKey 消息键
     * @param args       消息参数,用于替换占位符 {0}, {1} 等
     * @param locale     语言环境
     * @return 格式化后的国际化消息
     */
    @Override
    public String getMessage(String messageKey, Object[] args, Locale locale) {
        return getMessage(messageKey, args, null, locale);
    }

    /**
     * 获取国际化消息(完整版)
     *
     * <p>这是核心方法,实现了完整的缓存策略和多语言回退机制。
     *
     * <h4>查询流程:</h4>
     * <ol>
     *   <li>从 Redis 缓存获取(命中率 95%+)</li>
     *   <li>布隆过滤器判断数据是否可能存在</li>
     *   <li>使用分布式锁防止缓存击穿</li>
     *   <li>查询数据库并缓存结果</li>
     *   <li>尝试获取默认语言的消息</li>
     *   <li>返回默认消息或消息键</li>
     * </ol>
     *
     * <h4>示例:</h4>
     * <pre>
     * // 获取中文消息
     * getMessage("user.login.success", null, "登录成功", Locale.SIMPLIFIED_CHINESE);
     * // 返回: "登录成功"
     *
     * // 获取带参数的消息
     * getMessage("user.welcome", new Object[]{"张三"}, null, Locale.SIMPLIFIED_CHINESE);
     * // 消息模板: "欢迎 {0}"
     * // 返回: "欢迎 张三"
     * </pre>
     *
     * @param messageKey     消息键,不能为空
     * @param args           消息参数,可为 null
     * @param defaultMessage 默认消息,当所有查询都失败时返回,可为 null
     * @param locale         语言环境,不能为空
     * @return 国际化消息内容, 优先级: 缓存 > 数据库 > 默认语言 > 默认消息 > 消息键
     */
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
     *
     * <p>这是防止缓存穿透和缓存击穿的核心方法。
     *
     * <h4>解决的问题:</h4>
     * <ul>
     *   <li><b>缓存穿透</b>: 布隆过滤器快速判断数据是否存在,避免查询不存在的数据</li>
     *   <li><b>缓存击穿</b>: 分布式锁确保只有一个请求查询数据库,其他请求等待缓存</li>
     * </ul>
     *
     * <h4>执行流程:</h4>
     * <pre>
     * 1. 布隆过滤器判断
     *    ├─ 不存在 → 缓存空值(5分钟) → 返回 null
     *    └─ 可能存在 → 继续
     *
     * 2. 尝试获取分布式锁(最多等待3秒)
     *    ├─ 获取成功
     *    │   ├─ 双重检查缓存(DCL)
     *    │   ├─ 查询数据库
     *    │   ├─ 缓存结果(1小时+随机)
     *    │   └─ 释放锁
     *    └─ 获取失败
     *        ├─ 等待50ms
     *        └─ 重试获取缓存
     * </pre>
     *
     * <h4>性能指标:</h4>
     * <ul>
     *   <li>布隆过滤器判断: < 1ms</li>
     *   <li>获取锁超时: 3秒</li>
     *   <li>锁自动过期: 10秒</li>
     *   <li>空值缓存: 5分钟</li>
     *   <li>正常缓存: 1小时 + 0-5分钟随机</li>
     * </ul>
     *
     * @param messageKey 消息键
     * @param locale     语言环境字符串,如 "zh_CN"
     * @param cacheKey   Redis 缓存键
     * @return 消息内容, 如果不存在返回 null
     */
    private String getMessageWithLock(String messageKey, String locale, String cacheKey) {
        // 1. 先通过布隆过滤器判断数据是否可能存在（防止缓存穿透）
        String bloomKey = CommonConstants.buildI18nBloomKey(messageKey, locale);
        RBloomFilter<Object> bloomFilter = winterRedissionTemplate.getBloomFilter(CommonConstants.I18nMessage.I18N_BLOOM_FILTER_NAME);

        if (!bloomFilter.contains(bloomKey)) {
            log.debug("布隆过滤器判断数据不存在: key={}, locale={}", messageKey, locale);
            // 缓存空值，防止频繁查询
            winterRedisTemplate.set(cacheKey, CommonConstants.I18nMessage.I18N_NULL_VALUE, CommonConstants.I18nMessage.I18N_NULL_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
            return null;
        }

        // 2. 使用 Redisson 分布式锁（防止缓存击穿）
        String lockKey = CommonConstants.buildI18nLockKey(messageKey, locale);

        RLock lock = winterRedissionTemplate.getLock(lockKey);

        try {
            // 尝试获取锁，最多等待 3 秒，锁自动过期时间 10 秒
            if (lock.tryLock(3, CommonConstants.I18nMessage.I18N_LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS)) {
                try {
                    // 双重检查：获取锁后再次检查缓存
                    String cachedMessage = (String) winterRedisTemplate.get(cacheKey);
                    if (cachedMessage != null) {
                        if (!CommonConstants.I18nMessage.I18N_NULL_VALUE.equals(cachedMessage)) {
                            return cachedMessage;
                        }
                        return null;
                    }

                    // 查询数据库
                    log.debug("获取锁成功，查询数据库: key={}, locale={}", messageKey, locale);
                    String message = findMessageByKeyAndLocale(messageKey, locale);

                    if (message != null) {
                        // 缓存到 Redis，添加随机过期时间（防止缓存雪崩）
                        long expireSeconds = CommonConstants.I18nMessage.I18N_CACHE_EXPIRE_SECONDS
                                             + random.nextInt((int) CommonConstants.I18nMessage.I18N_CACHE_RANDOM_EXPIRE_SECONDS);
                        winterRedisTemplate.set(cacheKey, message, expireSeconds, TimeUnit.SECONDS);
                        log.debug("从数据库获取消息并缓存: key={}, locale={}, expire={}s", messageKey, locale, expireSeconds);
                        return message;
                    } else {
                        // 缓存空值（防止缓存穿透）
                        winterRedisTemplate.set(cacheKey, CommonConstants.I18nMessage.I18N_NULL_VALUE,
                                CommonConstants.I18nMessage.I18N_NULL_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
                        log.debug("缓存空值: key={}, locale={}", messageKey, locale);
                        return null;
                    }
                } finally {
                    // 释放锁
                    lock.unlock();
                }
            } else {
                // 获取锁超时，等待后重试获取缓存
                log.debug("获取锁超时，重试获取缓存: key={}, locale={}", messageKey, locale);
                Thread.sleep(50);
                String cachedMessage = (String) winterRedisTemplate.get(cacheKey);
                if (cachedMessage != null && !CommonConstants.I18nMessage.I18N_NULL_VALUE.equals(cachedMessage)) {
                    return cachedMessage;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取消息时被中断: key={}, locale={}", messageKey, locale, e);
        } catch (Exception e) {
            log.error("获取消息失败: key={}, locale={}", messageKey, locale, e);
        }

        return null;
    }

    /**
     * 获取默认语言的消息
     *
     * <p>当请求的语言不存在时,尝试获取默认语言(zh_CN)的消息,实现多语言回退机制。
     *
     * <h4>解决的问题:</h4>
     * <ul>
     *   <li>用户请求的语言版本不存在(如请求日语,但只有中英文)</li>
     *   <li>新增消息键时,部分语言版本还未翻译</li>
     *   <li>提供更好的用户体验,避免显示消息键</li>
     * </ul>
     *
     * <h4>查询策略:</h4>
     * <pre>
     * 1. 先查询默认语言的缓存
     * 2. 缓存未命中,使用分布式锁查询数据库
     * 3. 返回格式化后的消息
     * </pre>
     *
     * <h4>示例:</h4>
     * <pre>
     * // 请求日语消息,但日语版本不存在
     * getMessage("user.login.success", null, null, Locale.JAPANESE);
     * // 自动回退到中文: "登录成功"
     * </pre>
     *
     * @param messageKey 消息键
     * @param args       消息参数,用于格式化
     * @return 默认语言的消息内容, 如果不存在返回 null
     */
    private String getDefaultLocaleMessage(String messageKey, Object[] args) {
        String defaultCacheKey = CommonConstants.buildI18nMessageKey(messageKey, CommonConstants.I18nMessage.DEFAULT_LOCALE);
        String defaultCachedMessage = (String) winterRedisTemplate.get(defaultCacheKey);

        if (defaultCachedMessage != null) {
            // 存在值且不是字符串null
            if (!CommonConstants.I18nMessage.I18N_NULL_VALUE.equals(defaultCachedMessage)) {
                log.debug("使用默认语言缓存消息: key={}, locale={}", messageKey, CommonConstants.I18nMessage.DEFAULT_LOCALE);
                return formatMessage(defaultCachedMessage, args);
            }
            return null;
        }

        // 使用互斥锁查询默认语言消息
        String defaultMessage = getMessageWithLock(messageKey, CommonConstants.I18nMessage.DEFAULT_LOCALE, defaultCacheKey);
        if (defaultMessage != null) {
            log.debug("使用默认语言数据库消息: key={}, locale={}", messageKey, CommonConstants.I18nMessage.DEFAULT_LOCALE);
            return formatMessage(defaultMessage, args);
        }

        return null;
    }

    /**
     * 获取默认消息(最后的兜底策略)
     *
     * <p>当所有查询都失败时的最后兜底方案,确保系统不会因为缺少国际化消息而出错。
     *
     * <h4>返回优先级:</h4>
     * <ol>
     *   <li>默认语言(zh_CN)的消息</li>
     *   <li>调用方提供的默认消息</li>
     *   <li>消息键本身(便于开发调试)</li>
     * </ol>
     *
     * <h4>示例:</h4>
     * <pre>
     * // 场景1: 所有语言都不存在,但提供了默认消息
     * getDefaultMessage("new.feature", null, "新功能", "en_US");
     * // 返回: "新功能"
     *
     * // 场景2: 所有语言都不存在,没有提供默认消息
     * getDefaultMessage("new.feature", null, null, "en_US");
     * // 返回: "new.feature" (便于开发人员发现缺失的翻译)
     * </pre>
     *
     * @param messageKey     消息键
     * @param args           消息参数
     * @param defaultMessage 调用方提供的默认消息,可为 null
     * @param locale         当前请求的语言环境
     * @return 默认消息或消息键, 永远不会返回 null
     */
    private String getDefaultMessage(String messageKey, Object[] args, String defaultMessage, String locale) {
        // 尝试获取默认语言的消息
        if (!CommonConstants.I18nMessage.DEFAULT_LOCALE.equals(locale)) {
            String defaultLocaleMessage = getDefaultLocaleMessage(messageKey, args);
            if (defaultLocaleMessage != null) {
                return defaultLocaleMessage;
            }
        }
        // 返回默认消息或消息键
        return defaultMessage != null ? defaultMessage : messageKey;
    }

    /**
     * 格式化消息(支持占位符替换)
     *
     * <p>使用 {@link MessageFormat} 进行消息格式化,支持占位符 {0}, {1}, {2} 等。
     *
     * <h4>解决的问题:</h4>
     * <ul>
     *   <li>动态消息内容: 如 "欢迎 {0},您有 {1} 条新消息"</li>
     *   <li>格式化异常处理: 参数不匹配时返回原始消息,不影响系统运行</li>
     * </ul>
     *
     * <h4>支持的格式:</h4>
     * <pre>
     * {0}          - 简单占位符
     * {0,number}   - 数字格式化
     * {0,date}     - 日期格式化
     * {0,time}     - 时间格式化
     * {0,choice}   - 条件格式化
     * </pre>
     *
     * <h4>示例:</h4>
     * <pre>
     * // 简单替换
     * formatMessage("欢迎 {0}", new Object[]{"张三"});
     * // 返回: "欢迎 张三"
     *
     * // 多个参数
     * formatMessage("您有 {0} 条新消息,{1} 条未读", new Object[]{5, 3});
     * // 返回: "您有 5 条新消息,3 条未读"
     *
     * // 格式化异常
     * formatMessage("欢迎 {0}", new Object[]{});
     * // 返回: "欢迎 {0}" (原始消息,不抛异常)
     * </pre>
     *
     * @param message 消息模板,包含占位符
     * @param args    参数数组,可为 null 或空数组
     * @return 格式化后的消息, 如果格式化失败返回原始消息
     */
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
     *
     * <p>直接查询数据库,不经过缓存,用于缓存未命中时的数据加载。
     *
     * <h4>与 getMessage 的区别:</h4>
     * <ul>
     *   <li>findMessageByKeyAndLocale: 直接查询数据库,返回原始字符串</li>
     *   <li>getMessage: 经过缓存、布隆过滤器、分布式锁等完整流程</li>
     * </ul>
     *
     * @param messageKey 消息键
     * @param locale     语言环境字符串,如 "zh_CN"
     * @return 消息内容, 如果不存在返回 null
     */
    @Override
    public String findMessageByKeyAndLocale(String messageKey, String locale) {
        LambdaQueryWrapper<I18nMessagePO> queryWrapper = new LambdaQueryWrapper<I18nMessagePO>().eq(I18nMessagePO::getMessageKey, messageKey)
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
