package com.winter.cloud.i18n.infrastructure.repository;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.idev.excel.FastExcel;
import cn.idev.excel.support.ExcelTypeEnum;
import cn.idev.excel.write.handler.WriteHandler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.exception.BusinessException;
import com.winter.cloud.common.response.PageDTO;
import com.winter.cloud.common.response.Response;
import com.winter.cloud.common.util.TtlExecutorUtils;
import com.winter.cloud.dict.api.dto.command.DictCommand;
import com.winter.cloud.dict.api.dto.query.DictQuery;
import com.winter.cloud.dict.api.dto.response.DictDataDTO;
import com.winter.cloud.dict.api.facade.DictFacade;
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
import com.zsq.winter.office.entity.excel.*;
import com.zsq.winter.office.entity.excel.handler.CustomDateValidationWriteHandler;
import com.zsq.winter.office.entity.excel.handler.CustomMatchColumnWidthStyleHandler;
import com.zsq.winter.office.entity.excel.handler.CustomStyleHandler;
import com.zsq.winter.office.entity.excel.listener.WinterAnalysisValidReadListener;
import com.zsq.winter.office.service.excel.WinterExcelTemplate;
import com.zsq.winter.office.util.WebUtil;
import com.zsq.winter.redis.ddc.service.WinterRedisTemplate;
import com.zsq.winter.redis.ddc.service.WinterRedissionTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Validator;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 国际化消息仓储层实现类
 * <p>
 * 该类主要负责国际化数据的持久化、缓存管理以及第三方翻译接口调用。
 * 核心设计目标是在高并发场景下保证数据的一致性和查询的高性能。
 * <p>
 * <h3>核心架构特性：</h3>
 * <ul>
 * <li><strong>多级缓存架构：</strong> 采用 Redis 缓存作为一级缓存，并结合布隆过滤器和分布式锁构建防护层。</li>
 * <li><strong>缓存穿透防护：</strong> 使用 Redisson 布隆过滤器快速拦截不存在的 Key，防止无效请求直击数据库；同时对空结果进行短时缓存（缓存空对象）。</li>
 * <li><strong>缓存击穿防护：</strong> 在热点 Key 缓存失效时，使用 Redisson 分布式锁控制并发回源 DB，确保同一时刻只有一个线程查询数据库，防止数据库过载。</li>
 * <li><strong>缓存雪崩防护：</strong> 设置随机的缓存过期时间（Jitter），防止大量缓存同时过期导致请求激增。</li>
 * <li><strong>数据一致性：</strong> 放弃 @Transactional 注解，使用 {@link TransactionTemplate} 编程式事务。确保 DB 事务完全提交成功后，再执行 Redis 和布隆过滤器的更新，避免“脏读”和缓存与 DB 不一致。</li>
 * </ul>
 *
 * @author winter
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class I18nMessageRepositoryImpl implements I18nMessageRepository {

    private final Validator fastFalseValidator;
    // MyBatis Mapper 接口，负责底层 SQL 执行
    private final I18nMessageMapper messageMapper;
    // MyBatis-Plus Service 接口，提供便捷的 CRUD 操作
    private final II18nMessageMPService i18nMessageMPService;
    // 对象转换器，负责 PO (持久化对象) 与 DO (领域对象) 之间的转换
    private final I18nMessageInfraAssembler i18nMessageInfraAssembler;
    // 自定义 Redis 操作模版
    private final WinterRedisTemplate winterRedisTemplate;
    // Redisson 客户端，用于分布式锁和布隆过滤器
    private final WinterRedissionTemplate winterRedissionTemplate;
    private final ObjectMapper objectMapper;
    private final WinterExcelTemplate winterExcelTemplate;

    private final Executor i18bPoolExecutor;
    @DubboReference(check = false)
    private DictFacade dictFacade;
    /**
     * 编程式事务模版
     * <p>
     * 用于精确控制事务边界。解决 @Transactional 注解与缓存操作的时序问题。
     * 如果使用注解，缓存操作通常包裹在事务内，若缓存更新成功但后续 DB 提交失败，会导致缓存脏数据。
     */
    private final TransactionTemplate transactionTemplate;

    /**
     * 用于生成随机缓存过期时间，防止缓存雪崩
     */
    private final Random random = new Random();

    // ===== NVIDIA NIM 翻译服务配置 =====
    private final String apiKey = "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";
    private final String endpoint = "https://integrate.api.nvidia.com/v1/chat/completions";
    private final String model = "nvidia/riva-translate-4b-instruct-v1.1";

    // ===== 线程池 (TTL包装，支持上下文传递) =====
    // 使用 TtlExecutorUtils 包装线程池，确保 TraceId、UserContext 等 ThreadLocal 变量能在子线程中传递
    private final ExecutorService executor = TtlExecutorUtils.newFixedThreadPool(8);

    // ===== 语言代码映射表 (用于 Prompt 构建) =====
    private static final Map<String, String> LANG_MAP = new HashMap<>();

    static {
        LANG_MAP.put("zh_CN", "Chinese");
        LANG_MAP.put("en_US", "English");
        LANG_MAP.put("es_ES", "Spanish");
        LANG_MAP.put("fr_FR", "French");
        LANG_MAP.put("ru_RU", "Russian");
        LANG_MAP.put("ar_SA", "Arabic");
        LANG_MAP.put("ja_JP", "Japanese");
        LANG_MAP.put("ko_KR", "Korean");
        LANG_MAP.put("de_DE", "German");
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

    /**
     * 调用外部 API 进行多语言并发翻译
     *
     * @param command 翻译请求参数，包含源语言、目标语言列表和源文本
     * @return 翻译结果聚合对象
     * @throws ExecutionException   并发执行异常
     * @throws InterruptedException 线程中断异常
     */
    @Override
    public TranslateDO translate(TranslateCommand command) throws ExecutionException, InterruptedException {
        String sourceText = command.getSourceContent();
        String sourceLang = command.getSourceLanguage();
        List<String> targetLangs = command.getTargetLanguageList();

        // 使用 Future 并行调用翻译接口，提高响应速度
        // 每个目标语言启动一个异步任务
        List<Future<TranslateDO.TargetLanguageDTO>> futures = new ArrayList<>();
        for (String tgtLang : targetLangs) {
            futures.add(executor.submit(() -> translateOne(sourceLang, tgtLang, sourceText)));
        }

        // 收集所有异步任务的结果，阻塞等待所有任务完成
        Map<String, TranslateDO.TargetLanguageDTO> resultMap = new LinkedHashMap<>();
        for (Future<TranslateDO.TargetLanguageDTO> f : futures) {
            TranslateDO.TargetLanguageDTO dto = f.get(); // 阻塞获取结果
            resultMap.put(dto.getTargetLanguage(), dto);
        }

        // 构建返回对象
        return TranslateDO.builder()
                .sourceContent(sourceText)
                .sourceLanguage(sourceLang)
                .targetLanguageDTO(resultMap)
                .build();
    }

    @Override
    public PageDTO<I18nMessageDO> i18nPage(I18nMessageQuery i18nMessageQuery) {
        // 构建分页对象
        Page<I18nMessagePO> page = new Page<>(i18nMessageQuery.getPageNum(), i18nMessageQuery.getPageSize());
        // 执行分页查询
        IPage<I18nMessagePO> messagePage = messageMapper.selectI18nPage(page, i18nMessageQuery);
        // 将 PO 列表转换为 DO 列表
        List<I18nMessageDO> doList = i18nMessageInfraAssembler.toDOList(messagePage.getRecords());
        // 返回分页结果 DTO
        return new PageDTO<>(doList, messagePage.getTotal());
    }

    /**
     * 新增国际化消息
     * <p>
     * <h3>事务与缓存策略详解：</h3>
     * <ol>
     * <li>使用编程式事务 {@code transactionTemplate} 确保 DB 操作的原子性。</li>
     * <li>先在事务内进行数据转换、查重校验、DB 插入。</li>
     * <li><b>关键点：仅当 DB 事务提交成功后 (Boolean.TRUE.equals(dbSuccess))</b>，才执行 Redis 缓存更新和布隆过滤器添加。</li>
     * </ol>
     * 这样做是为了防止出现“数据库回滚了，但 Redis 却写入了数据”的脏读情况。
     *
     * @param command 新增参数
     * @return 操作是否成功
     */
    @Override
    public Boolean i18nSave(UpsertI18NCommand command) {
        // 1. 组装待保存的消息列表 (将 DTO 转换为 PO 实体)
        List<I18nMessagePO> messageList = command.getMessageValueList()
                .stream()
                .map(item -> I18nMessagePO.builder()
                        .id(null)
                        .type(command.getType())
                        .messageKey(command.getMessageKey())
                        .locale(item.getLocale())
                        .messageValue(item.getMessageValue())
                        .description(command.getDescription())
                        .build())
                .collect(Collectors.toList());

        // 2. 执行数据库事务 (包含业务逻辑校验和保存)
        Boolean dbSuccess = transactionTemplate.execute(status -> {
            List<String> localeList = messageList.stream()
                    .map(I18nMessagePO::getLocale)
                    .collect(Collectors.toList());

            // 构建查重条件：Type + MessageKey + Locale 必须唯一
            LambdaQueryWrapper<I18nMessagePO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(I18nMessagePO::getType, command.getType())
                    .eq(I18nMessagePO::getMessageKey, command.getMessageKey())
                    .in(I18nMessagePO::getLocale, localeList)
                    // 如果 ID 存在，排除自身（虽然是新增，但逻辑通用性考虑）
                    .ne(ObjectUtil.isNotEmpty(command.getId()), I18nMessagePO::getId, command.getId());

            List<I18nMessagePO> existList = i18nMessageMPService.list(queryWrapper);

            // 如果存在重复数据，抛出业务异常回滚事务
            if (!existList.isEmpty()) {
                List<String> existLocales = existList.stream()
                        .map(I18nMessagePO::getLocale)
                        .collect(Collectors.toList());
                throw new BusinessException(ResultCodeEnum.DUPLICATE_KEY_LANG.getCode(), "以下语言已存在配置，不可重复新增: " + existLocales);
            }

            // 批量保存到数据库
            return i18nMessageMPService.saveBatch(messageList);
        });

        // 3. 事务提交成功后，同步更新缓存和布隆过滤器
        if (Boolean.TRUE.equals(dbSuccess)) {
            try {
                // 获取布隆过滤器实例
                RBloomFilter<Object> bloomFilter = winterRedissionTemplate.getBloomFilter(CommonConstants.I18nMessage.I18N_BLOOM_FILTER_NAME);

                for (I18nMessagePO po : messageList) {
                    // A. 更新 Redis 缓存
                    // key 格式: i18n:message:{key}:{locale}
                    String cacheKey = CommonConstants.buildI18nMessageKey(po.getMessageKey(), po.getLocale());
                    // 设置随机过期时间，防止大量 Key 同时过期造成缓存雪崩
                    long expire = CommonConstants.I18nMessage.I18N_CACHE_EXPIRE_SECONDS
                                  + random.nextInt((int) CommonConstants.I18nMessage.I18N_CACHE_RANDOM_EXPIRE_SECONDS);
                    winterRedisTemplate.set(cacheKey, po.getMessageValue(), expire, TimeUnit.SECONDS);

                    // B. 添加到布隆过滤器
                    // 必须添加，否则 getMessage 方法的第一步布隆过滤器校验会失败，导致新数据被视为“非法请求”而直接拦截
                    String bloomKey = CommonConstants.buildI18nBloomKey(po.getMessageKey(), po.getLocale());
                    bloomFilter.add(bloomKey);
                }
            } catch (Exception e) {
                // 注意：缓存操作失败不应回滚主业务(因为DB已提交)，记录错误日志即可
                // 后续可依赖缓存过期自动修复，或通过定时任务/MQ进行补偿
                log.error("国际化新增成功但缓存同步失败: key={}", command.getMessageKey(), e);
            }
        }

        return dbSuccess;
    }

    /**
     * 检查是否存在重复的国际化消息
     */
    @Override
    public Boolean hasDuplicateI18nMessage(Long id, String locale, String messageKey, String type) {
        LambdaQueryWrapper<I18nMessagePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.nested(e ->
                e.eq(ObjectUtil.isNotEmpty(messageKey), I18nMessagePO::getMessageKey, messageKey)
                        .eq(ObjectUtil.isNotEmpty(locale), I18nMessagePO::getLocale, locale)
                        .eq(ObjectUtil.isNotEmpty(type), I18nMessagePO::getType, type)
                        // 排除自身 ID
                        .ne(ObjectUtil.isNotEmpty(id), I18nMessagePO::getId, id)
        );
        long count = i18nMessageMPService.count(queryWrapper);
        return count > 0;
    }

    /**
     * 更新国际化消息
     * <p>
     * 限制：为了数据安全和逻辑简化，目前限制一次只允许更新一个语言项。
     * <p>
     * <h3>策略：</h3>
     * <ul>
     * <li>先校验入参，确保一次只更新一条。</li>
     * <li>组装对象时<b>不设置 messageKey</b>，MyBatis-Plus 更新时忽略 null 字段，防止用户修改 Key 导致系统引用失效。</li>
     * <li>事务内更新 DB。</li>
     * <li>事务提交后，<b>覆盖更新 Redis</b>，并确保 Bloom Filter 包含该 Key。</li>
     * </ul>
     *
     * @param command 更新参数
     * @return 更新结果
     */
    @Override
    public Boolean i18nUpdate(UpsertI18NCommand command) {
        // 1. 组装数据并校验
        if (command.getMessageValueList().size() != 1) {
            throw new BusinessException(ResultCodeEnum.FAIL_LANG.getCode(), "不允许一个编号更新多个local的消息");
        }

        UpsertI18NCommand.MessageMap messageMap = command.getMessageValueList().get(0);

        // 注意：messageKey 不允许更新，所以 builder 中不要填充 messageKey 的值，
        // MyBatis-Plus 默认策略是 NOT_NULL，为 null 的字段不会被更新到 SQL 中。
        I18nMessagePO i18nMessagePO = I18nMessagePO.builder()
                .id(command.getId())
                .type(command.getType())
                .locale(messageMap.getLocale())
                .messageValue(messageMap.getMessageValue())
                .description(command.getDescription())
                .build();

        // 业务判重校验
        Boolean b = hasDuplicateI18nMessage(command.getId(), i18nMessagePO.getLocale(), command.getMessageKey(), command.getType());
        if (b) {
            throw new BusinessException(ResultCodeEnum.DUPLICATE_KEY_LANG.getCode(), "语言已存在配置，不可重复新增");
        }

        // 2. 执行数据库事务
        Boolean dbSuccess = transactionTemplate.execute(status -> {
            // 使用 updateById 更新单条记录
            return i18nMessageMPService.updateById(i18nMessagePO);
        });

        // 3. 事务提交成功后，覆盖缓存
        if (Boolean.TRUE.equals(dbSuccess)) {
            try {
                RBloomFilter<Object> bloomFilter = winterRedissionTemplate.getBloomFilter(CommonConstants.I18nMessage.I18N_BLOOM_FILTER_NAME);

                // A. 覆盖 Redis 缓存 (更新为最新值)
                // 这里使用 command.getMessageKey() 因为 PO 里没存 Key
                String cacheKey = CommonConstants.buildI18nMessageKey(command.getMessageKey(), i18nMessagePO.getLocale());
                long expire = CommonConstants.I18nMessage.I18N_CACHE_EXPIRE_SECONDS
                              + random.nextInt((int) CommonConstants.I18nMessage.I18N_CACHE_RANDOM_EXPIRE_SECONDS);
                winterRedisTemplate.set(cacheKey, i18nMessagePO.getMessageValue(), expire, TimeUnit.SECONDS);

                // B. 确保布隆过滤器包含该 Key (通常更新操作 Key 不变不需要此步，但为了防止 Locale 变更等边缘情况，补一下更安全)
                String bloomKey = CommonConstants.buildI18nBloomKey(command.getMessageKey(), i18nMessagePO.getLocale());
                bloomFilter.add(bloomKey);
            } catch (Exception e) {
                log.error("国际化更新成功但缓存同步失败: key={}", command.getMessageKey(), e);
            }
        }
        return dbSuccess;
    }

    /**
     * 删除国际化消息
     * <p>
     * <h3>策略：</h3>
     * <ul>
     * <li>先查询待删除数据（因为删完 DB 就查不到了，无法知道要删哪个 Redis Key）。</li>
     * <li>事务内删除 DB。</li>
     * <li>事务提交后，删除 Redis 缓存。</li>
     * <li><b>布隆过滤器处理：</b> 由于标准布隆过滤器不支持删除操作（会影响其他 Hash 冲突的 Key），此处<b>不做处理</b>。
     * 这会导致少量的“假阳性”（Bloom 认为存在，但 Redis/DB 实际已删除）。
     * 请求穿透到 DB 后查不到数据，会触发 {@code getMessageWithLock} 中的“空值缓存”逻辑，不会造成逻辑错误，只会增加一次 DB 空查询。</li>
     * </ul>
     *
     * @param ids 待删除的主键列表
     * @return 删除结果
     */
    @Override
    public Boolean i18nDelete(List<Long> ids) {
        // 用于在事务外删除缓存的数据容器
        List<I18nMessagePO> msgsToDelete = new ArrayList<>();

        // 1. 执行数据库事务
        Boolean dbSuccess = transactionTemplate.execute(status -> {
            // A. 先查询出要删除的记录，以便获取 messageKey 和 locale 用于拼装 Redis Key
            List<I18nMessagePO> list = i18nMessageMPService.listByIds(ids);
            if (CollUtil.isNotEmpty(list)) {
                msgsToDelete.addAll(list);
            }
            // B. 执行数据库物理删除
            return i18nMessageMPService.removeByIds(ids);
        });

        // 2. 事务提交成功后，删除 Redis 缓存
        if (Boolean.TRUE.equals(dbSuccess) && CollUtil.isNotEmpty(msgsToDelete)) {
            try {
                // 收集所有需要删除的 Cache Key
                List<String> cacheKeys = msgsToDelete.stream()
                        .map(msg -> CommonConstants.buildI18nMessageKey(msg.getMessageKey(), msg.getLocale()))
                        .collect(Collectors.toList());

                // 批量删除 Redis Key
                winterRedisTemplate.delete(cacheKeys);

                // 注意：布隆过滤器不支持删除操作，此处忽略。
                // 带来的影响是：已删除的 Key 仍会被布隆过滤器放行，穿透到 DB 查询。
                // 由于 DB 已删除，返回 null，缓存层会缓存空值 (NULL_VALUE)，不会造成业务错误。

            } catch (Exception e) {
                log.error("国际化删除成功但缓存清理失败: ids={}", ids, e);
            }
        }

        return dbSuccess;
    }

    @Override
    public void scheduledRebuildBloomFilter() {
        // 定义定时任务专用的锁 Key，避免与启动锁(WARMUP_LOCK)混用
        String lockKey = CommonConstants.I18nMessage.I18N_CACHE_WARMUP_LOCK_NAME + ":rebuild";
        RLock lock = winterRedissionTemplate.getLock(lockKey);

        try {
            // 尝试获取锁，不等待（如果其他节点正在跑，本节点直接跳过）
            // 租期设为 30 分钟，防止任务执行时间过长导致锁提前释放
            if (lock.tryLock(0, 30, TimeUnit.MINUTES)) {
                try {
                    long startTime = System.currentTimeMillis();
                    // 执行重建核心逻辑
                    // 注意：此处直接复用了 initBloomFilter，它会先 delete 再 init。
                    // 风险提示：在 delete 后到 init 完成前的毫秒级/秒级窗口期内，布隆过滤器为空，
                    // 此时所有请求的 contains 判断都会返回 false，导致请求被拦截（返回 null/默认值）。
                    // 建议：选择在业务低峰期（如凌晨 3-4 点）执行。
                    initBloomFilter();

                    // 可选：如果希望同时刷新 Redis 缓存防止冷数据，可取消注释下面一行
                    // warmupCache();
                    log.info("定时重建布隆过滤器完成，耗时: {}ms", System.currentTimeMillis() - startTime);
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                log.info("获取锁失败，其他节点正在执行重建任务，跳过。");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("定时任务被中断");
        } catch (Exception e) {
            log.error("定时重建布隆过滤器任务执行异常", e);
        }
    }

    @Override
    public void i18nExportExcel(HttpServletResponse response) {
        // 语言环境
        Response<List<DictDataDTO>> listLocaleResponse = dictFacade.dictValueDynamicQueryList(DictQuery.builder().dictTypeId(115L).build());
        List<DictDataDTO> localeList = listLocaleResponse.getData();
        // 语言环境映射
        Map<String, String> localeMap = localeList.stream().collect(Collectors.toMap(DictDataDTO::getDictValue, DictDataDTO::getDictLabel));

        // 类型
        Response<List<DictDataDTO>> listTypeResponse = dictFacade.dictValueDynamicQueryList(DictQuery.builder().dictTypeId(116L).build());
        List<DictDataDTO> typeList = listTypeResponse.getData();
        // 语言环境映射
        Map<String, String> typeMap = typeList.stream().collect(Collectors.toMap(DictDataDTO::getDictValue, DictDataDTO::getDictLabel));


        List<I18nMessagePO> collect = i18nMessageMPService.list().stream()
                .map(item -> {
                    item.setLocale(localeMap.get(item.getLocale()));
                    item.setType(typeMap.get(item.getType()));
                    return item;
                }).collect(Collectors.toList());
        ArrayList<WriteHandler> writeHandlers = new ArrayList<>();
        // 自定义样式处理器
        CustomStyleHandler cellStyleSheetWriteHandler = new CustomStyleHandler(null, null);
        writeHandlers.add(cellStyleSheetWriteHandler);
        writeHandlers.add(new CustomMatchColumnWidthStyleHandler());
        WinterExcelExportParam<I18nMessagePO> builder = WinterExcelExportParam.<I18nMessagePO>builder()
                .response(response)
                .batchSize(1000)
                .password("")
                .fileName("国际化消息.xlsx")
                .excludeColumnFieldNames(null)
                .writeHandlers(writeHandlers)
                .converters(null)
                .head(I18nMessagePO.class)
                .dataList(collect)
                .build();
        winterExcelTemplate.export(builder);
    }

    /**
     * 国际化信息 Excel 导入
     *
     * <p>
     * 功能说明：
     * 1. 从 Excel 中读取国际化消息数据
     * 2. 进行参数校验（JSR-303）
     * 3. 进行业务唯一性校验（messageKey + locale + type）
     * 4. 校验通过后逐条入库（事务控制）
     * 5. 入库成功后同步更新 Redis 缓存和布隆过滤器
     * 6. 如果存在校验/业务错误，则导出包含错误信息的 Excel
     * </p>
     *
     * @param response HTTP 响应，用于写出结果或错误 Excel
     * @param file     上传的 Excel 文件
     */
    @Override
    public void i18nImportExcel(HttpServletResponse response, MultipartFile file) throws IOException {

        // ====================== 1. 预加载字典数据 ======================
        // 语言环境字典（如：zh_CN -> 中文）
        Map<String, String> localeMap = dictCache("115", false);
        // 国际化类型字典
        Map<String, String> typeMap = dictCache("116", false);

        // Excel 多 Sheet 导出参数集合
        List<WinterExcelExportParam<?>> excelExportParamList = new ArrayList<>();

        // 业务逻辑错误集合（唯一性冲突等）
        List<WinterExcelBusinessErrorModel> winterExcelBusinessErrorModelList = new ArrayList<>();

        // ====================== 2. 构建 Excel 读取监听器 ======================
        /*
         * WinterAnalysisValidReadListener：
         * - 支持分批读取（这里每 1000 条回调一次）
         * - 内置 JSR-303 校验
         * - 回调的 item 接收到的数据一定是通过 JSR-303 校验的，永远不会包含校验失败的数据。
         *
         * 注意：
         * EasyExcel 使用反射创建实体对象，实体类必须提供可访问的无参构造器
         */
        WinterAnalysisValidReadListener<I18nMessagePO> i18nMessagePOAnalysisValidReadListener =
                new WinterAnalysisValidReadListener<>(1000, (item) -> {
                    // ====================== 3. 处理每一批校验通过的数据 ======================
                    for (I18nMessagePO i18nMessagePO : item) {
                        // 将 Excel 中的字典值转换为系统内部值
                        String localeOrDefault = localeMap.getOrDefault(i18nMessagePO.getLocale(), "");
                        String typeOrDefault = typeMap.getOrDefault(i18nMessagePO.getType(), "");
                        if (!ObjectUtil.isEmpty(localeOrDefault) && !ObjectUtil.isEmpty(typeOrDefault)) {
                            i18nMessagePO.setLocale(localeOrDefault);
                            i18nMessagePO.setType(typeOrDefault);
                        } else {
                            String jsonStr = null;
                            try {
                                jsonStr = objectMapper.writeValueAsString(i18nMessagePO);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                            WinterExcelBusinessErrorModel errorModel =
                                    WinterExcelBusinessErrorModel.builder()
                                            .errorMessage("国际化类型或者国际化语言环境字典映射错误！")
                                            .entityRowInfo(jsonStr)
                                            .build();

                            winterExcelBusinessErrorModelList.add(errorModel);
                        }
                        // 校验 messageKey + locale + type 是否已存在（唯一性校验）
                        Boolean hasDuplicate = hasDuplicateI18nMessage(
                                null,
                                i18nMessagePO.getLocale(),
                                i18nMessagePO.getMessageKey(),
                                i18nMessagePO.getType()
                        );

                        if (hasDuplicate) {
                            // ====================== 3.1 业务唯一性校验失败 ======================
                            try {
                                // 将当前行数据序列化，方便导出错误信息
                                String jsonStr = objectMapper.writeValueAsString(i18nMessagePO);

                                WinterExcelBusinessErrorModel errorModel =
                                        WinterExcelBusinessErrorModel.builder()
                                                .errorMessage("消息键、语言环境和类型组成的唯一内容已存在！")
                                                .entityRowInfo(jsonStr)
                                                .build();

                                winterExcelBusinessErrorModelList.add(errorModel);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }

                        } else {
                            // ====================== 3.2 业务校验通过，执行入库 ======================

                            /*
                             * 注意：
                             * 这里必须逐条插入，不能批量插入
                             * 原因：批量插入无法校验“当前批次内部”是否存在重复数据
                             */
                            Boolean dbSuccess = transactionTemplate.execute(status -> {
                                // 保存国际化消息
                                return i18nMessageMPService.save(i18nMessagePO);
                            });

                            // ====================== 3.3 数据库提交成功后，同步缓存 ======================
                            if (Boolean.TRUE.equals(dbSuccess)) {
                                try {
                                    // 获取布隆过滤器
                                    RBloomFilter<Object> bloomFilter =
                                            winterRedissionTemplate.getBloomFilter(
                                                    CommonConstants.I18nMessage.I18N_BLOOM_FILTER_NAME
                                            );

                                    // A. 覆盖 Redis 缓存
                                    String cacheKey = CommonConstants.buildI18nMessageKey(
                                            i18nMessagePO.getMessageKey(),
                                            i18nMessagePO.getLocale()
                                    );

                                    // 设置随机过期时间，防止缓存雪崩
                                    long expire = CommonConstants.I18nMessage.I18N_CACHE_EXPIRE_SECONDS
                                                  + random.nextInt(
                                            (int) CommonConstants.I18nMessage.I18N_CACHE_RANDOM_EXPIRE_SECONDS
                                    );

                                    winterRedisTemplate.set(
                                            cacheKey,
                                            i18nMessagePO.getMessageValue(),
                                            expire,
                                            TimeUnit.SECONDS
                                    );

                                    // B. 同步布隆过滤器（防止 locale / key 变更等边缘场景）
                                    String bloomKey = CommonConstants.buildI18nBloomKey(
                                            i18nMessagePO.getMessageKey(),
                                            i18nMessagePO.getLocale()
                                    );
                                    bloomFilter.add(bloomKey);

                                } catch (Exception e) {
                                    // 数据已入库，但缓存同步失败，仅记录日志，不回滚事务
                                    log.error("国际化更新成功但缓存同步失败: key={}",
                                            i18nMessagePO.getMessageKey(), e);
                                }
                            }
                        }
                    }
                }, fastFalseValidator, CollUtil.toList(I18nMessagePO.Import.class));

        // ====================== 4. 执行 Excel 读取 ======================
        FastExcel.read(file.getInputStream(), I18nMessagePO.class, i18nMessagePOAnalysisValidReadListener)
                .excelType(ExcelTypeEnum.XLSX)
                .password("")
                .sheet(0)
                .doRead();

        // ====================== 5. 收集校验错误信息 ======================
        // JSR-303 校验错误
        List<WinterExcelValidateErrorModel> errorList =
                i18nMessagePOAnalysisValidReadListener.getErrorList();

        // ====================== 6. 构建业务逻辑错误 Sheet ======================
        if (!ObjectUtils.isEmpty(winterExcelBusinessErrorModelList)) {

            WinterExcelExportParam<WinterExcelBusinessErrorModel> businessParam =
                    WinterExcelExportParam.<WinterExcelBusinessErrorModel>builder()
                            .sheetName("业务逻辑错误信息")
                            .excludeColumnFieldNames(new ArrayList<>())
                            .writeHandlers(new ArrayList<>())
                            .password("")
                            .dataList(winterExcelBusinessErrorModelList)
                            .head(WinterExcelBusinessErrorModel.class)
                            .build();

            excelExportParamList.add(businessParam);
        }

        // ====================== 7. 构建校验逻辑错误 Sheet（支持国际化） ======================
        if (!ObjectUtils.isEmpty(errorList)) {

            // 对校验错误信息进行国际化处理
            errorList.forEach(error -> {
                String message = error.getMessage();
                StringBuilder stringBuilder = new StringBuilder();

                // 判断是否为国际化 key 格式：{xxx}
                if (message != null && message.startsWith("{") && message.endsWith("}")) {
                    String messageKey = message.substring(1, message.length() - 1);
                    try {
                        stringBuilder.append(getMessage(messageKey, new Object[]{}, message));
                    } catch (Exception ex) {
                        // 国际化失败，使用原始消息
                        stringBuilder.append(message);
                    }
                } else {
                    stringBuilder.append(message);
                }
                error.setMessage(stringBuilder.toString());
            });

            WinterExcelExportParam<WinterExcelValidateErrorModel> validateParam =
                    WinterExcelExportParam.<WinterExcelValidateErrorModel>builder()
                            .sheetName("校验逻辑错误信息")
                            .excludeColumnFieldNames(new ArrayList<>())
                            .writeHandlers(new ArrayList<>())
                            .password("")
                            .dataList(errorList)
                            .head(WinterExcelValidateErrorModel.class)
                            .build();

            excelExportParamList.add(validateParam);
        }

        // ====================== 8. 返回结果 ======================
        if (ObjectUtils.isEmpty(winterExcelBusinessErrorModelList)
            && ObjectUtils.isEmpty(errorList)) {

            // 无任何错误，直接返回成功结果
            Response<Object> build = Response.build(null, "200", "导入成功！");
            WebUtil.renderString(response, JSONUtil.toJsonStr(build));

        } else {
            // 存在错误，导出多 Sheet 的错误 Excel
            winterExcelTemplate.exportMultiSheet(
                    response,
                    "错误信息.xlsx",
                    "",
                    excelExportParamList
            );
        }
    }


    @Override
    public void i18nExportExcelTemplate(HttpServletResponse response) {
        ArrayList<WriteHandler> writeHandlers = new ArrayList<>();
        // 自定义样式处理器
        CustomStyleHandler cellStyleSheetWriteHandler = new CustomStyleHandler(null, null);
        writeHandlers.add(new CustomDateValidationWriteHandler(Map.of(5, new WinterDateValidationModel("yyyy-MM-dd HH:mm:ss", 2, 5555))));
        writeHandlers.add(cellStyleSheetWriteHandler);
        writeHandlers.add(new CustomMatchColumnWidthStyleHandler());
        WinterExcelExportParam<I18nMessagePO> builder = WinterExcelExportParam.<I18nMessagePO>builder()
                .response(response)
                .batchSize(1000)
                .password("")
                .fileName("国际化消息模版.xlsx")
                // 导出的模版不需要创建时间这个列
                .excludeColumnFieldNames(List.of("createTime"))
                .converters(null)
                .writeHandlers(writeHandlers)
                .head(I18nMessagePO.class)
                .dataList(null)
                .build();
        winterExcelTemplate.export(builder);
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
     * <p>
     * 流程：Redis 查询 -> (未命中) -> 布隆过滤器校验 -> 分布式锁 -> DB 查询 -> 回写 Redis
     *
     * @param messageKey     消息键
     * @param args           参数数组 (用于占位符替换)
     * @param defaultMessage 默认消息
     * @param locale         语言环境
     * @return 格式化后的消息
     */
    @Override
    public String getMessage(String messageKey, Object[] args, String defaultMessage, Locale locale) {
        // 构造标准的 Locale 字符串，例如 zh_CN
        String localeStr = locale.getLanguage() + (locale.getCountry().isEmpty() ? "" : "_" + locale.getCountry());

        // 异步统计逻辑 - 支持区分语种
        CompletableFuture.runAsync(() -> {
            try {
                if (ObjectUtil.isNotEmpty(messageKey)) {
                    // 构造复合 Member: "messageKey:locale"
                    // 示例: "user.login.success:zh_CN"
                    String memberWithLocale = messageKey + ":" + localeStr;
                    // 统计带有语言后缀的 Key
                    winterRedisTemplate.zSetIncrementScore(CommonConstants.I18nMessage.I18N_STAT_KEY, memberWithLocale, 1);

                    // (可选) 如果你还想看“不分语言的总热度”，可以再多记一条：
                    // winterRedisTemplate.opsForZSet().incrementScore(I18N_STAT_KEY + ":global", messageKey, 1);
                }
            } catch (Exception e) {
                log.warn("Async stat i18n failed: key={}", messageKey, e);
            }
        }, i18bPoolExecutor);

        // 构建 Redis Key
        String cacheKey = CommonConstants.buildI18nMessageKey(messageKey, localeStr);
        try {
            // 1. 一级缓存查询 (Redis)
            String cachedMessage = (String) winterRedisTemplate.get(cacheKey);
            if (cachedMessage != null) {
                // 检查是否是防穿透的"空值占位符" (例如 "NULL_VALUE")
                if (CommonConstants.I18nMessage.I18N_NULL_VALUE.equals(cachedMessage)) {
                    log.debug("命中空值缓存: key={}, locale={}", messageKey, localeStr);
                    return defaultMessage != null ? defaultMessage : messageKey;
                }
                log.debug("从缓存获取消息: key={}, locale={}", messageKey, localeStr);
                // 格式化消息并返回
                return formatMessage(cachedMessage, args);
            }

            // 2. 二级保护查询（布隆 + 锁 + DB），防止缓存击穿和穿透
            String message = getMessageWithLock(messageKey, localeStr, cacheKey);
            if (message != null) {
                return formatMessage(message, args);
            }

        } catch (Exception e) {
            // 缓存异常降级：记录日志，返回默认值，不影响主流程
            log.error("获取国际化消息失败: key={}, locale={}", messageKey, localeStr, e);
        }

        return defaultMessage != null ? defaultMessage : messageKey;
    }

    /**
     * 带锁的回源查询逻辑
     * <p>
     * 包含：布隆过滤器校验、分布式锁竞争、双重检查锁 (DCL)、空值缓存
     */
    private String getMessageWithLock(String messageKey, String locale, String cacheKey) {
        String bloomKey = CommonConstants.buildI18nBloomKey(messageKey, locale);
        RBloomFilter<Object> bloomFilter = winterRedissionTemplate.getBloomFilter(CommonConstants.I18nMessage.I18N_BLOOM_FILTER_NAME);

        // 1. 布隆过滤器前置校验 (防穿透核心)
        // 如果布隆过滤器说不存在，那一定不存在，直接返回 null，无需查 DB
        if (!bloomFilter.contains(bloomKey)) {
            log.debug("布隆过滤器判断数据不存在: key={}, locale={}", messageKey, locale);
            // 即使布隆过滤掉，也建议写入短期的 NULL 缓存，应对海量恶意请求绕过布隆过滤器的情况
            winterRedisTemplate.set(cacheKey, CommonConstants.I18nMessage.I18N_NULL_VALUE, CommonConstants.I18nMessage.I18N_NULL_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
            return null;
        }

        // 2. 获取分布式锁 (防击穿核心)
        // 只有拿到锁的线程才能去查 DB，其他线程等待
        String lockKey = CommonConstants.buildI18nLockKey(messageKey, locale);
        RLock lock = winterRedissionTemplate.getLock(lockKey);

        try {
            // 尝试获取锁，等待 3 秒，锁定 10 秒（防止死锁）
            if (lock.tryLock(3, CommonConstants.I18nMessage.I18N_LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS)) {
                try {
                    // Double check (DCL)：拿到锁后再次检查 Redis
                    // 可能前一个持有锁的线程已经把数据放入缓存了，此时直接返回即可，无需查 DB
                    String cachedMessage = (String) winterRedisTemplate.get(cacheKey);
                    if (cachedMessage != null) {
                        return !CommonConstants.I18nMessage.I18N_NULL_VALUE.equals(cachedMessage) ? cachedMessage : null;
                    }

                    // 3. 回源查询 DB
                    log.debug("获取锁成功，查询数据库: key={}, locale={}", messageKey, locale);
                    String message = findMessageByKeyAndLocale(messageKey, locale);

                    if (message != null) {
                        // DB 查到了，写入 Redis，设置随机过期时间 (Jitter) 防止雪崩
                        long expireSeconds = CommonConstants.I18nMessage.I18N_CACHE_EXPIRE_SECONDS
                                             + random.nextInt((int) CommonConstants.I18nMessage.I18N_CACHE_RANDOM_EXPIRE_SECONDS);
                        winterRedisTemplate.set(cacheKey, message, expireSeconds, TimeUnit.SECONDS);
                        return message;
                    } else {
                        // DB 也没查到，说明是"假阳性"或数据真的没了
                        // 写入空值缓存，防止再次查 DB (缓存穿透防护)
                        winterRedisTemplate.set(cacheKey, CommonConstants.I18nMessage.I18N_NULL_VALUE,
                                CommonConstants.I18nMessage.I18N_NULL_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
                        return null;
                    }
                } finally {
                    lock.unlock(); // 释放锁
                }
            } else {
                // 自旋等待逻辑：如果没拿到锁，说明有别的线程正在查 DB
                // 稍微休眠一下再查缓存，而不是直接报错
                log.debug("获取锁超时，进入自旋等待: key={}, locale={}", messageKey, locale);
                for (int i = 0; i < 3; i++) {
                    Thread.sleep(50);
                    String cachedMessage = (String) winterRedisTemplate.get(cacheKey);
                    // 如果拿到锁的线程已经写入了数据，这里就能查到了
                    if (cachedMessage != null && !CommonConstants.I18nMessage.I18N_NULL_VALUE.equals(cachedMessage)) {
                        return cachedMessage;
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取消息失败: key={}, locale={}", messageKey, locale, e);
        }
        return null;
    }

    /**
     * 格式化消息参数 (MessageFormat.format)
     */
    private String formatMessage(String message, Object[] args) {
        if (args != null && args.length > 0) {
            try {
                return MessageFormat.format(message, args);
            } catch (Exception e) {
                // 格式化失败时降级返回原消息，避免报错影响主业务
                log.warn("消息格式化失败: message={}, args={}", message, Arrays.toString(args), e);
                return message;
            }
        }
        return message;
    }

    @Override
    public String findMessageByKeyAndLocale(String messageKey, String locale) {
        LambdaQueryWrapper<I18nMessagePO> queryWrapper = new LambdaQueryWrapper<I18nMessagePO>()
                .eq(I18nMessagePO::getMessageKey, messageKey)
                .eq(I18nMessagePO::getLocale, locale);
        I18nMessagePO i18nMessagePO = messageMapper.selectOne(queryWrapper);
        return ObjectUtil.isNotEmpty(i18nMessagePO) ? i18nMessagePO.getMessageValue() : null;
    }

    @Override
    public List<I18nMessageDO> getI18nMessageInfo(I18nMessageQuery query) {
        List<I18nMessagePO> i18nMessageInfo = messageMapper.getI18nMessageInfo(query);
        return i18nMessageInfraAssembler.toDOList(i18nMessageInfo);
    }


    // ===== 单条翻译逻辑 =====

    /**
     * 单条文本翻译实现 (调用 NVIDIA API)
     */
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

        // 发送 HTTP 请求到 NVIDIA 翻译服务
        String response = HttpRequest.post(endpoint)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(20000)
                .body(body.toString())
                .execute()
                .body();

        // 解析响应
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


    /**
     * 核心逻辑：将数据库数据加载到 Redis (分页版)
     * <p>
     * 改为分页查询，防止数据量过大导致 OOM
     */
    public void warmupCache() {
        try {
            log.info("开始执行 Redis 缓存预热...");
            long startTime = System.currentTimeMillis();

            int pageNum = 1;
            int pageSize = 1000; // 每批处理 1000 条，可根据服务器内存调整
            int totalSuccess = 0;

            while (true) {
                // 1. 构造分页对象
                Page<I18nMessagePO> page = new Page<>(pageNum, pageSize);

                // 2. 执行分页查询 (直接复用 MP Service 的 page 方法，无需手写 SQL)
                IPage<I18nMessagePO> pageResult = i18nMessageMPService.page(page);
                List<I18nMessagePO> records = pageResult.getRecords();

                // 如果查询结果为空，说明已处理完所有数据，跳出循环
                if (CollUtil.isEmpty(records)) {
                    break;
                }

                // 3. 遍历当前页数据并写入 Redis
                for (I18nMessagePO message : records) {
                    try {
                        // 构建缓存 Key: "i18n:message:{key}:{locale}"
                        String cacheKey = CommonConstants.buildI18nMessageKey(
                                message.getMessageKey(), message.getLocale());

                        // 设置随机过期时间，防止缓存雪崩
                        long expireSeconds = CommonConstants.I18nMessage.I18N_CACHE_EXPIRE_SECONDS
                                             + random.nextInt((int) CommonConstants.I18nMessage.I18N_CACHE_RANDOM_EXPIRE_SECONDS);

                        // 写入 Redis (修正：统一使用 winterRedisTemplate，与 getMessage/i18nSave 保持一致)
                        winterRedisTemplate.set(
                                cacheKey,
                                message.getMessageValue(),
                                expireSeconds,
                                TimeUnit.SECONDS);

                        totalSuccess++;
                    } catch (Exception e) {
                        // 单条数据失败不影响整体
                        log.error("预热消息失败: key={}, locale={}",
                                message.getMessageKey(), message.getLocale(), e);
                    }
                }

                // 4. 准备下一页
                pageNum++;
            }

            log.info("Redis 缓存预热完成: 耗时={}ms, 成功写入={}条",
                    System.currentTimeMillis() - startTime, totalSuccess);

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
            RBloomFilter<Object> bloomFilter = winterRedissionTemplate.getBloomFilter(CommonConstants.I18nMessage.I18N_BLOOM_FILTER_NAME);

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
            List<I18nMessageDO> allMessages = getI18nMessageInfo(null);

            if (ObjectUtil.isEmpty(allMessages)) {
                log.warn("没有找到需要加载到布隆过滤器的消息");
                return;
            }

            int addCount = 0;
            // 4. 将所有存在的 Key + Locale 组合写入布隆过滤器
            for (I18nMessageDO message : allMessages) {
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


    /**
     * 根据字典类型获取字典键值对
     *
     * <p>
     * 默认返回：label -> value
     * 当 reverse = true 时返回：value -> label
     * </p>
     *
     * <p>
     * 查询优先级：
     * 1. 先从 Redis 缓存中获取
     * 2. 缓存不存在或解析失败，则调用远程字典服务获取
     * </p>
     *
     * @param dictType 字典类型（字典类型 ID）
     * @param reverse  是否反转 key / value
     * @return 字典键值 Map，不存在则返回空 Map
     */
    public Map<String, String> dictCache(String dictType, boolean reverse) {

        // Redis 中字典缓存的 key，格式：DICT_KEY:dictType
        String redisKey = CommonConstants.Redis.DICT_KEY
                          + CommonConstants.Redis.SPLIT
                          + dictType;

        // ====================== 1. 优先从 Redis 中获取字典数据 ======================
        Object cache = winterRedisTemplate.get(redisKey);

        // 将 Redis 中的缓存数据反序列化为字典列表
        List<DictDataDTO> dictList = parseFromCache(cache);

        // ====================== 2. 缓存未命中或解析失败时，调用远程服务 ======================
        if (CollUtil.isEmpty(dictList)) {
            dictList = fetchFromRemote(dictType);
        }

        // ====================== 3. 按是否反转构建返回的 Map ======================
        return buildResultMap(dictList, reverse);
    }

    /**
     * 从 Redis 缓存中解析字典数据
     *
     * <p>
     * Redis 中存储的是字典数据的 JSON 字符串，
     * 这里负责将其反序列化为 List<DictDataDTO>
     * </p>
     *
     * @param cache Redis 获取到的缓存对象
     * @return 字典列表，解析失败或缓存为空时返回空集合
     */
    private List<DictDataDTO> parseFromCache(Object cache) {

        // 缓存为空，直接返回空集合
        if (ObjectUtil.isEmpty(cache)) {
            return Collections.emptyList();
        }

        try {
            // 将 JSON 字符串反序列化为字典数据列表
            return objectMapper.readValue(
                    cache.toString(),
                    new TypeReference<List<DictDataDTO>>() {
                    }
            );
        } catch (Exception e) {
            // JSON 解析失败时记录日志，避免问题被悄悄吞掉
            log.warn("解析字典缓存失败，缓存内容：{}", cache, e);
            return Collections.emptyList();
        }
    }

    /**
     * 从远程字典服务中获取字典数据
     *
     * <p>
     * 远程接口返回的是：
     * Map<dictType, List<DictDataDTO>>
     * 这里需要将所有字典数据打平，并过滤出当前 dictType 对应的数据
     * </p>
     *
     * @param dictType 字典类型（字典类型 ID）
     * @return 字典列表，远程调用失败时返回空集合
     */
    private List<DictDataDTO> fetchFromRemote(String dictType) {

        // 调用字典微服务，根据字典类型查询字典数据
        Response<Map<String, List<DictDataDTO>>> response =
                dictFacade.getDictDataByType(new DictCommand(Long.valueOf(dictType), "1"));

        // 接口返回为空，直接返回空集合，避免空指针
        if (ObjectUtil.isEmpty(response) || ObjectUtil.isEmpty(response.getData())) {
            return Collections.emptyList();
        }

        // 将 Map 中的所有字典列表打平，并过滤出当前 dictType 的字典数据
        return response.getData()
                .values()
                .stream()
                .flatMap(List::stream)
                .filter(d -> dictType.equals(String.valueOf(d.getDictTypeId())))
                .collect(Collectors.toList());
    }

    /**
     * 根据字典列表构建最终返回的 Map
     *
     * <p>
     * 不反转（reverse = false）：label -> value
     * 反转（reverse = true）：value -> label
     * </p>
     *
     * @param list    字典数据列表
     * @param reverse 是否反转 key / value
     * @return 构建好的字典 Map
     */
    private Map<String, String> buildResultMap(List<DictDataDTO> list, boolean reverse) {

        // 字典列表为空时，返回空 Map
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyMap();
        }

        return list.stream()
                .collect(Collectors.toMap(
                        // 根据 reverse 决定 key 的取值
                        reverse ? DictDataDTO::getDictValue : DictDataDTO::getDictLabel,
                        // 根据 reverse 决定 value 的取值
                        reverse ? DictDataDTO::getDictLabel : DictDataDTO::getDictValue,
                        // 当 key 冲突时，保留第一个值，防止抛出异常
                        (v1, v2) -> v1
                ));
    }
}
