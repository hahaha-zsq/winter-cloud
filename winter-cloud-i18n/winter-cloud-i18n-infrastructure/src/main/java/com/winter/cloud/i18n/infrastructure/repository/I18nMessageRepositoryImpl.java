package com.winter.cloud.i18n.infrastructure.repository;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.exception.BusinessException;
import com.winter.cloud.common.response.PageDTO;
import com.winter.cloud.common.util.TtlExecutorUtils;
import com.winter.cloud.i18n.api.dto.command.TranslateCommand;
import com.winter.cloud.i18n.api.dto.command.UpsertI18NCommand;
import com.winter.cloud.i18n.api.dto.query.I18nMessageQuery;
import com.winter.cloud.i18n.domain.model.entity.I18nMessageDO;
import com.winter.cloud.i18n.domain.model.entity.TranslateDO;
import com.winter.cloud.i18n.domain.repository.I18nMessageRepository;
import com.winter.cloud.i18n.infrastructure.assembler.I18nMessageInfraAssembler;
import com.winter.cloud.i18n.infrastructure.entity.I18nMessagePO;
import com.winter.cloud.i18n.infrastructure.mapper.I18nMessageMapper;
import com.winter.cloud.i18n.infrastructure.service.II18nMessageMPService;
import com.zsq.winter.redis.ddc.service.WinterRedisTemplate;
import com.zsq.winter.redis.ddc.service.WinterRedissionTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;


@Repository
@Slf4j
@RequiredArgsConstructor
public class I18nMessageRepositoryImpl implements I18nMessageRepository {
    private final I18nMessageMapper messageMapper;
    private final II18nMessageMPService i18nMessageMPService;
    private final I18nMessageInfraAssembler i18nMessageInfraAssembler;
    private final WinterRedisTemplate winterRedisTemplate;
    private final WinterRedissionTemplate winterRedissionTemplate;
    private final Random random = new Random();

    // ===== NVIDIA NIM 配置 =====
    private final String apiKey = "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";
    private final String endpoint = "https://integrate.api.nvidia.com/v1/chat/completions";
    private final String model = "nvidia/riva-translate-4b-instruct-v1.1";

    // ===== 线程池 =====
    private final ExecutorService executor = TtlExecutorUtils.newFixedThreadPool(8);

    // ===== 语言映射表 =====
    private static final Map<String, String> LANG_MAP = new HashMap<>();

    static {
        // ===== 简体中文 =====
        LANG_MAP.put("zh_CN", "Chinese");
        // ===== 英语 =====
        LANG_MAP.put("en_US", "English");
        // ===== 西班牙语 =====
        LANG_MAP.put("es_ES", "Spanish");
        // ===== 法语 =====
        LANG_MAP.put("fr_FR", "French");
        // ===== 俄语 =====
        LANG_MAP.put("ru_RU", "Russian");
        // ===== 阿拉伯语 =====
        LANG_MAP.put("ar_SA", "Arabic");
        // ===== 日语 =====
        LANG_MAP.put("ja_JP", "Japanese");
        // ===== 韩语 =====
        LANG_MAP.put("ko_KR", "Korean");
        // ===== 德语 =====
        LANG_MAP.put("de_DE", "German");
        // ===== 意大利语 =====
        LANG_MAP.put("pt_BR", "Brazilian Portuguese");
    }

    @Override
    public String getMessage(String messageKey) {
        return getMessage(messageKey, null, null, LocaleContextHolder.getLocale());
    }


    @Override
    public String getMessage(String messageKey, Object[] args) {
        return getMessage(messageKey, args, null, LocaleContextHolder.getLocale());

    }

    @Override
    public String getMessage(String messageKey, Object[] args, String defaultMessage) {
        return getMessage(messageKey, args, defaultMessage, LocaleContextHolder.getLocale());
    }

    @Override
    public TranslateDO translate(TranslateCommand command) throws ExecutionException, InterruptedException {
        // ====== NVIDIA NIM 配置 ======

        String sourceText = command.getSourceContent();
        String sourceLang = command.getSourceLanguage();
        List<String> targetLangs = command.getTargetLanguageList();

        // 并发执行翻译任务
        List<Future<TranslateDO.TargetLanguageDTO>> futures = new ArrayList<>();

        for (String tgtLang : targetLangs) {
            futures.add(executor.submit(() -> translateOne(sourceLang, tgtLang, sourceText)));
        }

        // 组装返回DTO
        Map<String, TranslateDO.TargetLanguageDTO> resultMap = new LinkedHashMap<>();

        for (Future<TranslateDO.TargetLanguageDTO> f : futures) {
            TranslateDO.TargetLanguageDTO dto = f.get();
            resultMap.put(dto.getTargetLanguage(), dto);
        }

        return TranslateDO.builder()
                .sourceContent(sourceText)
                .sourceLanguage(sourceLang)
                .targetLanguageDTO(resultMap)
                .build();
    }

    @Override
    public PageDTO<I18nMessageDO> i18nPage(I18nMessageQuery i18nMessageQuery) {
        // 1. 构建分页对象
        Page<I18nMessagePO> page = new Page<>(i18nMessageQuery.getPageNum(), i18nMessageQuery.getPageSize());
        IPage<I18nMessagePO> messagePage = messageMapper.selectI18nPage(page, i18nMessageQuery);
        List<I18nMessageDO> doList = i18nMessageInfraAssembler.toDOList(messagePage.getRecords());
        return new PageDTO<>(doList, messagePage.getTotal());
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean i18nSave(UpsertI18NCommand command) {
        // 1. 组装待保存的消息列表
        List<I18nMessagePO> messageList = command.getMessageValueList()
                .stream()
                .map(item -> I18nMessagePO.builder()
                        .id(ObjectUtil.isNotEmpty(command.getId())?command.getId():null)
                        .type(command.getType())
                        .messageKey(command.getMessageKey())
                        .locale(item.getLocale())
                        .messageValue(item.getMessageValue())
                        .description(command.getDescription())
                        .build())
                .collect(Collectors.toList());

        // 2. 提取所有 locale 集合，用于批量查重
        List<String> localeList = messageList.stream()
                .map(I18nMessagePO::getLocale)
                .collect(Collectors.toList());

        // 3. 一次性查询数据库中已存在的记录
        LambdaQueryWrapper<I18nMessagePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(I18nMessagePO::getType, command.getType())
                .eq(I18nMessagePO::getMessageKey, command.getMessageKey())
                .in(I18nMessagePO::getLocale, localeList)
                .ne(ObjectUtil.isNotEmpty(command.getId()),I18nMessagePO::getId, command.getId());

        List<I18nMessagePO> existList = i18nMessageMPService.list(queryWrapper);

        // 4. 若存在重复，则直接抛出业务异常
        if (!existList.isEmpty()) {
            List<String> existLocales = existList.stream()
                    .map(I18nMessagePO::getLocale)
                    .collect(Collectors.toList());

            throw new BusinessException(ResultCodeEnum.DUPLICATE_KEY_LANG.getCode(),"以下语言已存在配置，不可重复新增: " + existLocales);
        }

        // 5. 批量保存
        return i18nMessageMPService.saveBatch(messageList);
    }

    /**
     * 校验是否存在重复的国际化消息配置
     *
     * 校验规则：
     * 同一 type（前端/后端） + messageKey（消息键） + locale（语言环境）
     * 在数据库中只能存在一条记录。
     *
     * 使用场景：
     * - 新增时：id 为空，检查是否已存在相同组合的数据
     * - 编辑时：id 不为空，排除自身记录后检查是否重复
     *
     * @param id          当前记录ID（编辑时传入，新增时可为空）
     * @param locale      语言环境（如 zh_CN, en_US）
     * @param messageKey 消息键（如 login.success）
     * @param type        类型（1:后端，2:前端）
     * @return true: 存在重复记录  false: 不存在重复记录
     */
    @Override
    public Boolean hasDuplicateI18nMessage(Long id, String locale, String messageKey, String type) {

        // 构建 Lambda 查询条件
        LambdaQueryWrapper<I18nMessagePO> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.nested(e ->
                // 按消息键匹配（若参数不为空才拼接条件）
                e.eq(ObjectUtil.isNotEmpty(messageKey), I18nMessagePO::getMessageKey, messageKey)
                        // 按语言环境匹配
                        .eq(ObjectUtil.isNotEmpty(locale), I18nMessagePO::getLocale, locale)
                        // 按类型匹配（前端/后端）
                        .eq(ObjectUtil.isNotEmpty(type), I18nMessagePO::getType, type)
                        // 编辑场景下排除自身记录，避免误判
                        .ne(ObjectUtil.isNotEmpty(id), I18nMessagePO::getId, id)
        );

        // 查询符合条件的数据条数
        long count = i18nMessageMPService.count(queryWrapper);

        // count > 0 表示存在重复配置
        return count > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean i18nUpdate(UpsertI18NCommand command) {
        // 1. 组装待保存的消息列表
        List<I18nMessagePO> messageList = command.getMessageValueList()
                .stream()
                .map(item -> I18nMessagePO.builder()
                        .id(command.getId())
                        .type(command.getType())
                        .messageKey(command.getMessageKey())
                        .locale(item.getLocale())
                        .messageValue(item.getMessageValue())
                        .description(command.getDescription())
                        .build())
                .collect(Collectors.toList());

        // 2. 提取所有 locale 集合，用于批量查重
        List<String> localeList = messageList.stream()
                .map(I18nMessagePO::getLocale)
                .collect(Collectors.toList());

        // 3. 一次性查询数据库中已存在的记录
        LambdaQueryWrapper<I18nMessagePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(I18nMessagePO::getType, command.getType())
                .eq(I18nMessagePO::getMessageKey, command.getMessageKey())
                .in(I18nMessagePO::getLocale, localeList)
                .ne(ObjectUtil.isNotEmpty(command.getId()),I18nMessagePO::getId, command.getId());

        List<I18nMessagePO> existList = i18nMessageMPService.list(queryWrapper);

        // 4. 若存在重复，则直接抛出业务异常
        if (!existList.isEmpty()) {
            List<String> existLocales = existList.stream()
                    .map(I18nMessagePO::getLocale)
                    .collect(Collectors.toList());

            throw new BusinessException(ResultCodeEnum.DUPLICATE_KEY_LANG.getCode(),"以下语言已存在配置，不可重复新增: " + existLocales);
        }

        // 5. 批量保存
        return i18nMessageMPService.updateBatchById(messageList);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean i18nDelete(List<Long> ids) {
        return i18nMessageMPService.removeByIds(ids);
    }


    @Override
    public String getMessage(String messageKey, Locale locale) {
        return getMessage(messageKey, null, null, locale);
    }

    @Override
    public String getMessage(String messageKey, Object[] args, Locale locale) {
        return getMessage(messageKey, args, null, locale);
    }

    /**
     * 获取国际化消息的核心方法
     *
     * <p>执行步骤：
     * <ol>
     * <li><b>Redis 查询：</b>尝试从 Redis 缓存获取消息，命中则直接格式化返回。</li>
     * <li><b>空值检查：</b>如果命中空值缓存，直接返回兜底值，不走后续流程。</li>
     * <li><b>回源查询：</b>缓存未命中时，调用 {@link #getMessageWithLock} 使用分布式锁和布隆过滤器安全地回源查询。</li>
     * <li><b>最终兜底：</b>如果仍未获取到，直接返回 {@code defaultMessage} 或 {@code messageKey}，<b>不进行默认语言回退查询</b>。</li>
     * </ol>
     *
     * @param messageKey     消息键 (e.g., "user.login.success")
     * @param args           参数数组 (用于替换 {0}, {1})
     * @param defaultMessage 默认消息（当查不到数据时的返回值）
     * @param locale         语言环境 (e.g., zh_CN)
     * @return 格式化后的国际化消息字符串
     */
    @Override
    public String getMessage(String messageKey, Object[] args, String defaultMessage, Locale locale) {
        // 1. 将 Locale 对象转换为字符串（例如 "zh_CN"）
        String localeStr = locale.getLanguage()
                           + (locale.getCountry().isEmpty() ? "" : "_" + locale.getCountry());
        // 从 Java 9 开始，Locale.toString() 会把 Script 信息（比如 Hans、Hant）加到 Variant 里，用 _# 表示。会解析出zh_CN_#Hans，所以为了兼容jdk11,使用 locale.getLanguage() + locale.getCountry() 组合
        // String localeStr = locale.toString();
        // 2. 构建 Redis 缓存 Key，格式通常为 "i18n:message:{messageKey}:{locale}"
        // 示例：winter:i18n:message:user.login.success:zh_CN
        String cacheKey = CommonConstants.buildI18nMessageKey(messageKey, localeStr);

        try {
            // =================================================================================
            // 3. 一级缓存查询：直接从 Redis 获取
            // =================================================================================
            String cachedMessage = (String) winterRedisTemplate.get(cacheKey);

            // 3.1 缓存命中 (Hit)
            if (cachedMessage != null) {
                // 特殊处理：检查是否命中“空值占位符”（防止缓存穿透）
                // 如果 Redis 中存的是特定的空值标记（如 "null" 字符串），说明 DB 中确实没有该数据
                // 此时直接返回兜底值，不再尝试查询默认语言，避免无意义的性能消耗
                if (CommonConstants.I18nMessage.I18N_NULL_VALUE.equals(cachedMessage)) {
                    log.debug("命中空值缓存: key={}, locale={}", messageKey, localeStr);
                    return defaultMessage != null ? defaultMessage : messageKey;
                }

                // 命中有效数据，进行参数格式化（替换 {0}, {1} 等占位符）后返回
                log.debug("从缓存获取消息: key={}, locale={}", messageKey, localeStr);
                return formatMessage(cachedMessage, args);
            }

            // =================================================================================
            // 4. 二级保护查询：缓存未命中，进入“布隆过滤器 + 分布式锁”流程
            // =================================================================================
            // 调用 getMessageWithLock 方法：
            // 1. 检查布隆过滤器（不存在则直接返回 null 并缓存空值）
            // 2. 获取分布式锁（防止热点 Key 击穿）
            // 3. 查询数据库并回写 Redis
            String message = getMessageWithLock(messageKey, localeStr, cacheKey);

            // 如果回源查询成功，格式化并返回
            if (message != null) {
                return formatMessage(message, args);
            }

            // [优化] 已移除原有的“自动降级查询默认语言”策略
            // 原因：防止恶意请求不存在的 Key 时，引发对默认语言缓存/DB的二次无效查询，减轻系统负载。

        } catch (Exception e) {
            // 捕获所有异常，确保国际化功能的故障不会影响主业务流程，仅记录错误日志
            log.error("获取国际化消息失败: key={}, locale={}", messageKey, localeStr, e);
        }

        // =================================================================================
        // 5. 最终兜底返回 (Final Fallback)
        // =================================================================================
        // 如果上述所有步骤都失败（缓存无、DB无、或发生异常）：
        // 优先返回调用方提供的默认消息 defaultMessage；如果也没提供，则直接返回 messageKey 本身
        return defaultMessage != null ? defaultMessage : messageKey;
    }

    /**
     * 在缓存未命中时，使用布隆过滤器和分布式锁安全地从数据库获取消息。
     *
     * <p>设计目的：高并发场景下的多级缓存保护</p>
     * <ul>
     * <li><b>防穿透：</b>BloomFilter 过滤不存在的 Key，直接拦截。</li>
     * <li><b>防击穿：</b>Redisson 分布式锁控制并发，保证只有一个线程去查 DB。</li>
     * <li><b>防雪崩：</b>回写缓存时设置随机过期时间。</li>
     * <li><b>自旋优化：</b>锁等待超时后不直接放弃，而是自旋读缓存。</li>
     * </ul>
     *
     * @param messageKey 消息键
     * @param locale     语言环境字符串
     * @param cacheKey   Redis 完整缓存 Key
     * @return 消息内容，若不存在则返回 null
     */
    private String getMessageWithLock(String messageKey, String locale, String cacheKey) {
        // =================================================================================
        // 1. 第一层保护：布隆过滤器 (Bloom Filter)
        // 目的：防止“缓存穿透”。拦截数据库中根本不存在的 key，避免无效请求打到数据库。
        // =================================================================================

        // 构建布隆过滤器的 Key（注意：这个 Key 必须与预热时存入布隆过滤器的规则一致）
        String bloomKey = CommonConstants.buildI18nBloomKey(messageKey, locale);
        // 获取 Redisson 的布隆过滤器实例
        RBloomFilter<Object> bloomFilter = winterRedissionTemplate.getBloomFilter(CommonConstants.I18nMessage.I18N_BLOOM_FILTER_NAME);

        // check: 如果布隆过滤器判断元素不存在，那它一定不存在（无假阴性）
        if (!bloomFilter.contains(bloomKey)) {
            log.debug("布隆过滤器判断数据不存在: key={}, locale={}", messageKey, locale);
            // 策略：缓存空值 (Cache Null Object)
            // 即使数据不存在，也暂时缓存一个空标记，防止短时间内大量恶意请求重复穿透布隆过滤器
            winterRedisTemplate.set(cacheKey, CommonConstants.I18nMessage.I18N_NULL_VALUE, CommonConstants.I18nMessage.I18N_NULL_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
            return null;
        }

        // =================================================================================
        // 2. 第二层保护：分布式锁 (Distributed Lock)
        // 目的：防止“缓存击穿”。当热点 Key 失效时，防止成千上万的并发请求同时击穿数据库。
        // =================================================================================

        // 元素可能存在（布隆过滤器有误判率，所以必须走后续流程），准备获取锁
        String lockKey = CommonConstants.buildI18nLockKey(messageKey, locale);
        // 获取锁（此处使用 Redisson 锁，锁定粒度为“key + locale”）
        RLock lock = winterRedissionTemplate.getLock(lockKey);

        try {
            // tryLock 参数说明：
            // waitTime (3s): 尝试获取锁的最大等待时间。如果 3 秒拿不到锁，说明竞争非常激烈或数据库处理慢。
            // leaseTime (10s): 锁的自动释放时间。防止服务宕机导致死锁。
            if (lock.tryLock(3, CommonConstants.I18nMessage.I18N_LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS)) {
                try {
                    // =============================================================================
                    // 3. 双重检查 (Double Check Locking)
                    // 目的：性能优化。在等待锁的过程中，可能持有锁的前一个线程已经把数据加载到 Redis 了。
                    // =============================================================================
                    String cachedMessage = (String) winterRedisTemplate.get(cacheKey);
                    if (cachedMessage != null) {
                        // 如果缓存中已经是“空值标记”，说明前一个线程查库后发现数据确实不存在
                        if (!CommonConstants.I18nMessage.I18N_NULL_VALUE.equals(cachedMessage)) {
                            return cachedMessage;
                        }
                        return null;
                    }

                    // =============================================================================
                    // 4. 回源查询 (Source of Truth)
                    // 此时确定缓存无数据，且我是唯一持有锁的线程，可以安全查询数据库
                    // =============================================================================
                    log.debug("获取锁成功，查询数据库: key={}, locale={}", messageKey, locale);
                    String message = findMessageByKeyAndLocale(messageKey, locale);

                    // =============================================================================
                    // 5. 回写缓存 (Write Back)
                    // =============================================================================
                    if (message != null) {
                        // 策略：随机过期时间 (Random Expiration)
                        // 目的：防止“缓存雪崩”。在基础过期时间上增加随机值，避免大量 Key 在同一时刻集体失效。
                        long expireSeconds = CommonConstants.I18nMessage.I18N_CACHE_EXPIRE_SECONDS
                                             + random.nextInt((int) CommonConstants.I18nMessage.I18N_CACHE_RANDOM_EXPIRE_SECONDS);

                        // 写入有效数据
                        winterRedisTemplate.set(cacheKey, message, expireSeconds, TimeUnit.SECONDS);
                        log.debug("从数据库获取消息并缓存: key={}, locale={}, expire={}s", messageKey, locale, expireSeconds);
                        return message;
                    } else {
                        // 策略：再次缓存空值
                        // 即使 DB 返回 null（布隆过滤器误判），也要缓存空标记，防止持续穿透
                        winterRedisTemplate.set(cacheKey, CommonConstants.I18nMessage.I18N_NULL_VALUE,
                                CommonConstants.I18nMessage.I18N_NULL_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
                        log.debug("缓存空值: key={}, locale={}", messageKey, locale);
                        return null;
                    }
                } finally {
                    // 必须在 finally 块中释放锁，保证任何异常情况下锁都能被释放
                    lock.unlock();
                }
            } else {
                // =============================================================================
                // 6. 锁超时处理：自旋等待 (Spin Wait)
                // 目的：高并发降级策略。
                // 场景：如果获取锁超时，大概率是因为持有锁的线程正在查询 DB。
                // 此时直接返回 null 体验不好。不如稍微等一下（自旋），因为 DB 查询通常很快，马上就能从缓存拿到了。
                // =============================================================================
                log.debug("获取锁超时，进入自旋等待: key={}, locale={}", messageKey, locale);

                // 自旋 3 次，每次间隔 50ms (总计等待约 150ms)
                for (int i = 0; i < 3; i++) {
                    Thread.sleep(50); // 主动让出 CPU
                    // 每次醒来都尝试读一下缓存
                    String cachedMessage = (String) winterRedisTemplate.get(cacheKey);
                    // 如果读到了有效值，直接返回，挽救了一次查询失败
                    if (cachedMessage != null && !CommonConstants.I18nMessage.I18N_NULL_VALUE.equals(cachedMessage)) {
                        log.debug("自旋等待后命中缓存: key={}, locale={}", messageKey, locale);
                        return cachedMessage;
                    }
                }
                // 如果自旋结束后还是拿不到数据，只能放弃
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

    /**
     * 格式化消息字符串
     * <p>使用 {@link MessageFormat} 将参数填充到消息模板中。</p>
     * * @param message 消息模板 (e.g., "Hello {0}")
     *
     * @param args 参数数组 (e.g., ["World"])
     * @return 格式化后的字符串 (e.g., "Hello World")
     */
    private String formatMessage(String message, Object[] args) {
        if (args != null && args.length > 0) {
            try {
                return MessageFormat.format(message, args);
            } catch (Exception e) {
                // 格式化失败时降级返回原始消息，不抛出异常
                log.warn("消息格式化失败: message={}, args={}", message, Arrays.toString(args), e);
                return message;
            }
        }
        return message;
    }

    /**
     * 根据消息键和语言环境查询消息内容
     * <p>直接查询数据库,不经过缓存。</p>
     *
     * @param messageKey 消息键
     * @param locale     语言环境
     * @return 消息值
     * @apiNote 注意：此方法不走缓存，仅用于缓存未命中时的回源查询，请勿在高频业务中直接调用。
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


    // ===== 单条翻译 =====
    private TranslateDO.TargetLanguageDTO translateOne(String srcLang,
                                                       String tgtLang,
                                                       String text) {

        JSONObject body = new JSONObject();
        body.set("model", model);
        body.set("temperature", 0);
        body.set("top_p", 0.9);
        body.set("max_tokens", 512);
        body.set("stream", false);

        JSONArray messages = new JSONArray();

        JSONObject sys = new JSONObject();
        sys.set("role", "system");
        sys.set("content", buildSystemPrompt(srcLang, tgtLang));

        JSONObject user = new JSONObject();
        user.set("role", "user");
        user.set("content", buildUserPrompt(tgtLang, text));

        messages.add(sys);
        messages.add(user);
        body.set("messages", messages);

        String response = HttpRequest.post(endpoint)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(20000)
                .body(body.toString())
                .execute()
                .body();

        JSONObject json = new JSONObject(response);
        String translatedText = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getStr("content");

        TranslateDO.TargetLanguageDTO dto = new TranslateDO.TargetLanguageDTO();
        dto.setTargetLanguage(tgtLang);
        dto.setTargetContent(translatedText);

        return dto;
    }

    // ===== Prompt 构造 =====
    private String buildSystemPrompt(String srcCode, String tgtCode) {
        return String.format(
                "You are an expert at translating text from %s to %s.",
                LANG_MAP.get(srcCode),
                LANG_MAP.get(tgtCode)
        );
    }

    private String buildUserPrompt(String tgtCode, String text) {
        return String.format(
                "What is the %s translation of the sentence: %s",
                LANG_MAP.get(tgtCode),
                text
        );
    }

}