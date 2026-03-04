package com.winter.cloud.auth.infrastructure.repository;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.idev.excel.FastExcel;
import cn.idev.excel.support.ExcelTypeEnum;
import cn.idev.excel.write.handler.WriteHandler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winter.cloud.auth.api.dto.query.PostQuery;
import com.winter.cloud.auth.domain.model.entity.AuthPostDO;
import com.winter.cloud.auth.domain.model.entity.AuthRoleDO;
import com.winter.cloud.auth.domain.repository.AuthPostRepository;
import com.winter.cloud.auth.infrastructure.assembler.AuthPostInfraAssembler;
import com.winter.cloud.auth.infrastructure.entity.AuthPostPO;
import com.winter.cloud.auth.infrastructure.entity.AuthRolePO;
import com.winter.cloud.auth.infrastructure.entity.AuthUserPO;
import com.winter.cloud.auth.infrastructure.mapper.AuthPostMapper;
import com.winter.cloud.auth.infrastructure.service.IAuthPostMPService;
import com.winter.cloud.auth.infrastructure.service.IAuthUserMpService;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.exception.BusinessException;
import com.winter.cloud.common.response.PageDTO;
import com.winter.cloud.common.response.Response;
import com.winter.cloud.dict.api.dto.command.DictCommand;
import com.winter.cloud.dict.api.dto.response.DictDataDTO;
import com.winter.cloud.dict.api.facade.DictFacade;
import com.zsq.i18n.template.WinterI18nTemplate;
import com.zsq.winter.office.entity.excel.WinterExcelBusinessErrorModel;
import com.zsq.winter.office.entity.excel.WinterExcelExportParam;
import com.zsq.winter.office.entity.excel.WinterExcelValidateErrorModel;
import com.zsq.winter.office.entity.excel.handler.CustomMatchColumnWidthStyleHandler;
import com.zsq.winter.office.entity.excel.handler.CustomStyleHandler;
import com.zsq.winter.office.entity.excel.listener.WinterAnalysisValidReadListener;
import com.zsq.winter.office.service.excel.WinterExcelTemplate;
import com.zsq.winter.office.util.WebUtil;
import com.zsq.winter.redis.ddc.service.WinterRedisTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Validator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.winter.cloud.common.enums.ResultCodeEnum.DUPLICATE_KEY;

/**
 * 职位仓储实现类
 * <p>
 * 负责职位数据的持久化操作，包括职位的增删改查、导入导出等功能。
 * <p>
 * 主要功能：
 * <ul>
 *   <li>职位动态查询：根据条件动态查询职位列表或单个职位</li>
 *   <li>职位分页查询：支持分页和排序的职位查询</li>
 *   <li>职位保存/更新：新增或更新职位信息，包含重复性校验</li>
 *   <li>职位删除：删除职位，支持关联用户检查</li>
 *   <li>Excel导入导出：支持职位的Excel模板导出、数据导入和批量导出</li>
 *   <li>字典缓存：提供字典数据的缓存和远程获取功能</li>
 * </ul>
 *
 * @author winter
 * @since 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AuthPostRepositoryImpl implements AuthPostRepository {

    private final IAuthPostMPService authPostMpService;
    private final IAuthUserMpService authUserMpService;
    private final AuthPostMapper authPostMapper;
    private final AuthPostInfraAssembler authPostInfraAssembler;
    private final WinterI18nTemplate winterI18nTemplate;
    private final WinterExcelTemplate winterExcelTemplate;
    private final WinterRedisTemplate winterRedisTemplate;
    private final ObjectMapper objectMapper;
    private final Validator fastFalseValidator;
    /**
     * 编程式事务模板
     * <p>
     * 用于精确控制事务边界。解决 @Transactional 注解与缓存操作的时序问题。
     * 如果使用注解，缓存操作通常包裹在事务内，若缓存更新成功但后续 DB 提交失败，会导致缓存脏数据。
     */
    private final TransactionTemplate transactionTemplate;
    @DubboReference(check = false)
    private DictFacade dictFacade;

    /**
     * 根据条件动态查询职位列表
     *
     * @param postQuery 查询条件，支持按ID、职位编码、职位名称、状态进行模糊/精确查询
     * @return 符合条件的职位领域对象列表
     */
    @Override
    public List<AuthPostDO> postDynamicQueryList(PostQuery postQuery) {
        LambdaQueryWrapper<AuthPostPO> queryWrapper = new LambdaQueryWrapper<AuthPostPO>()
                .eq(ObjectUtil.isNotEmpty(postQuery.getId()), AuthPostPO::getId, postQuery.getId())
                .like(ObjectUtil.isNotEmpty(postQuery.getPostCode()), AuthPostPO::getPostCode, postQuery.getPostCode())
                .like(ObjectUtil.isNotEmpty(postQuery.getPostName()), AuthPostPO::getPostName, postQuery.getPostName())
                .eq(ObjectUtil.isNotEmpty(postQuery.getStatus()), AuthPostPO::getStatus, postQuery.getStatus());
        List<AuthPostPO> list = authPostMpService.list(queryWrapper);
        return authPostInfraAssembler.toDOList(list);
    }

    /**
     * 根据条件动态查询单个职位
     *
     * @param postQuery 查询条件，支持按ID、职位编码、职位名称、状态进行模糊/精确查询
     * @return 符合条件的职位领域对象，若不存在则返回null
     */
    @Override
    public AuthPostDO postDynamicQuery(PostQuery postQuery) {
        LambdaQueryWrapper<AuthPostPO> queryWrapper = new LambdaQueryWrapper<AuthPostPO>()
                .eq(ObjectUtil.isNotEmpty(postQuery.getId()), AuthPostPO::getId, postQuery.getId())
                .like(ObjectUtil.isNotEmpty(postQuery.getPostCode()), AuthPostPO::getPostCode, postQuery.getPostCode())
                .like(ObjectUtil.isNotEmpty(postQuery.getPostName()), AuthPostPO::getPostName, postQuery.getPostName())
                .eq(ObjectUtil.isNotEmpty(postQuery.getStatus()), AuthPostPO::getStatus, postQuery.getStatus());
        AuthPostPO authPostPO = authPostMpService.getOne(queryWrapper);
        return authPostInfraAssembler.toDO(authPostPO);
    }

    /**
     * 判断是否存在重复的职位
     * <p>
     * 检查规则：职位名相同 或 职位编码相同<br>
     * 如果是更新操作（aDo.getId()不为空），则排除自身
     *
     * @param aDo 职位领域对象，包含职位名、职位编码等信息
     * @return true表示存在重复职位，false表示不存在重复
     */
    @Override
    public Boolean hasDuplicatePost(AuthPostDO aDo) {
        LambdaQueryWrapper<AuthPostPO> authPostPOLambdaQueryWrapper = new LambdaQueryWrapper<>();
        authPostPOLambdaQueryWrapper.nested(
                e -> e.eq(AuthPostPO::getPostName, aDo.getPostName())
                        .or()
                        .eq(AuthPostPO::getPostCode, aDo.getPostCode()))
                .ne(ObjectUtil.isNotEmpty(aDo.getId()), AuthPostPO::getId, aDo.getId());
        long count = authPostMpService.count(authPostPOLambdaQueryWrapper);
        return count > 0;
    }

    /**
     * 分页查询职位列表
     *
     * @param postQuery 查询条件，包含分页参数（pageNum、pageSize）和排序参数
     * @return 分页结果，包含职位列表和总数
     */
    @Override
    public PageDTO<AuthPostDO> postPage(PostQuery postQuery) {
        // 1. 构建分页对象
        Page<AuthUserPO> page = new Page<>(postQuery.getPageNum(), postQuery.getPageSize());
        IPage<AuthPostPO> postPage = authPostMapper.selectPostPage(page, postQuery);
        List<AuthPostDO> doList = authPostInfraAssembler.toDOList(postPage.getRecords());
        return new PageDTO<>(doList, postPage.getTotal());
    }

    /**
     * 保存职位信息
     * <p>
     * 在保存前会进行重复性校验，如果存在相同名称或编码的职位则抛出业务异常
     *
     * @param aDo 职位领域对象
     * @return 保存是否成功
     * @throws BusinessException 如果职位名称或编码已存在
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean postSave(AuthPostDO aDo) {
        boolean b = this.hasDuplicatePost(aDo);
        if (b) {
            throw new BusinessException(DUPLICATE_KEY.getCode(), winterI18nTemplate.message(CommonConstants.I18nKey.POST_NAME_OR_IDENTIFIER_EXISTS));
        }
        AuthPostPO authPostPO = authPostInfraAssembler.toPO(aDo);
        return authPostMpService.save(authPostPO);
    }

    /**
     * 更新职位信息
     * <p>
     * 在更新前会进行重复性校验，如果存在相同名称或编码的职位则抛出业务异常。
     * 更新时不允许修改职位编码（职位编码设为null保留原值）
     *
     * @param aDo 职位领域对象
     * @return 更新是否成功
     * @throws BusinessException 如果职位名称或编码已存在
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean postUpdate(AuthPostDO aDo) {
        boolean b = this.hasDuplicatePost(aDo);
        if (b) {
            throw new BusinessException(DUPLICATE_KEY.getCode(), winterI18nTemplate.message(CommonConstants.I18nKey.POST_NAME_OR_IDENTIFIER_EXISTS));
        }
        AuthPostPO authPostPO = authPostInfraAssembler.toPO(aDo);
        // 更新时不允许修改职位编码
        authPostPO.setPostCode(null);
        return authPostMpService.updateById(authPostPO);
    }

    /**
     * 删除职位信息
     * <p>
     * 支持批量删除，会检查职位是否关联了用户。如果职位已关联用户，则无法删除。
     * 只有未被用户关联的职位才会被实际删除。
     *
     * @param postIdList 职位ID列表
     * @return 删除是否成功
     * @throws BusinessException 如果所选职位均已关联用户，或删除失败
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean postDelete(List<Long> postIdList) {
        if (ObjectUtil.isEmpty(postIdList)) {
            return false;
        }

        // 允许删除的职位编号集合
        List<Long> allowDeletePostIds;
        // 查询被用户关联的职位
        List<AuthUserPO> list = authUserMpService.list(
                new LambdaQueryWrapper<AuthUserPO>().in(AuthUserPO::getPostId, postIdList)
        );

        if (ObjectUtil.isNotEmpty(list)) {
            // 收集被用户关联的职位
            List<Long> collect = list.stream().map(AuthUserPO::getPostId).collect(Collectors.toList());
            // 过滤掉被用户关联的职位
            allowDeletePostIds = postIdList.stream().filter(postId -> !collect.contains(postId)).collect(Collectors.toList());
        } else {
            allowDeletePostIds = postIdList;
        }

        // 1. 拦截空集合：如果没有可以删除的职位，直接中断处理
        if (ObjectUtil.isEmpty(allowDeletePostIds)) {
            // 建议在这里抛出自定义业务异常，提示前端 "所选职位均已关联用户，无法删除"
            throw new BusinessException(ResultCodeEnum.FAIL_LANG.getCode(), winterI18nTemplate.message("Post.cannot.delete"));
        }

        // 2. 执行主表删除
        boolean b = authPostMpService.removeByIds(allowDeletePostIds);
        if (!b) {
            // 主表删除失败，必须抛出异常触发回滚！
            throw new BusinessException(ResultCodeEnum.FAIL_LANG.getCode(), winterI18nTemplate.message("delete.fail")); // 替换为你项目中的实际业务异常类
        }
        return true;
    }

    /**
     * 导出职位Excel模板
     * <p>
     * 导出一个空白的Excel模板，用于用户填写职位信息进行导入。
     * 模板不包含创建时间列
     *
     * @param response HTTP响应对象，用于输出Excel文件
     */
    @Override
    public void postExportExcelTemplate(HttpServletResponse response) {
        ArrayList<WriteHandler> writeHandlers = new ArrayList<>();
        // 自定义样式处理器
        CustomStyleHandler cellStyleSheetWriteHandler = new CustomStyleHandler(null, null);
        writeHandlers.add(cellStyleSheetWriteHandler);
        writeHandlers.add(new CustomMatchColumnWidthStyleHandler());
        WinterExcelExportParam<AuthPostPO> builder = WinterExcelExportParam.<AuthPostPO>builder()
                .response(response)
                .batchSize(1000)
                .password("")
                .fileName(winterI18nTemplate.message(CommonConstants.I18nKey.POST_INFORMATION_TEMPLATE)+".xlsx")
                // 导出的模版不需要创建时间这个列
                .excludeColumnFieldNames(List.of("createTime"))
                .converters(null)
                .writeHandlers(writeHandlers)
                .head(AuthPostPO.class)
                .dataList(null)
                .build();
        winterExcelTemplate.export(builder);
    }

    /**
     * 导入职位Excel数据
     * <p>
     * 支持从Excel文件中批量导入职位数据，包含以下功能：
     * <ul>
     *   <li>字典值转换：将Excel中的状态显示值转换为系统内部值</li>
     *   <li>JSR-303校验：对每条数据进行字段校验</li>
     *   <li>业务唯一性校验：检查职位名称和编码是否重复</li>
     * </ul>
     * 导入结果：
     * <ul>
     *   <li>全部成功：返回成功响应</li>
     *   <li>部分失败：导出包含错误信息的Excel文件（多Sheet）</li>
     * </ul>
     *
     * @param response HTTP响应对象，用于输出导入结果或错误Excel
     * @param file 上传的Excel文件
     * @throws IOException 如果读取文件发生IO异常
     */
    @Override
    public void postImportExcel(HttpServletResponse response, MultipartFile file) throws IOException {

        // ====================== 1. 预加载字典数据 ======================
        Map<String, String> statusMap = dictCache("110", false);

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
        WinterAnalysisValidReadListener<AuthPostPO> analysisValidReadListener =
                new WinterAnalysisValidReadListener<>(1000, (item) -> {
                    // ====================== 3. 处理每一批校验通过的数据 ======================
                    for (AuthPostPO authPostPO : item) {
                        // 将 Excel 中的字典值转换为系统内部值
                        String statusOrDefault = statusMap.getOrDefault(authPostPO.getStatus(), "");
                        if (!ObjectUtil.isEmpty(statusOrDefault)) {
                            authPostPO.setStatus(statusOrDefault);
                        } else {
                            String jsonStr = null;
                            try {
                                jsonStr = objectMapper.writeValueAsString(authPostPO);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                            WinterExcelBusinessErrorModel errorModel =
                                    WinterExcelBusinessErrorModel.builder()
                                            .errorMessage(winterI18nTemplate.message(CommonConstants.I18nKey.STATUS_DICT_MAPPING_ERROR))
                                            .entityRowInfo(jsonStr)
                                            .build();
                            winterExcelBusinessErrorModelList.add(errorModel);
                        }
                        // 校验是否已存在（唯一性校验）
                        boolean hasDuplicate = hasDuplicatePost(
                                AuthPostDO.builder()
                                        .postCode(authPostPO.getPostCode())
                                        .postName(authPostPO.getPostName())
                                        .status(authPostPO.getStatus())
                                        .id(authPostPO.getId())
                                        .build());

                        if (hasDuplicate) {
                            // ====================== 3.1 业务唯一性校验失败 ======================
                            try {
                                // 将当前行数据序列化，方便导出错误信息
                                String jsonStr = objectMapper.writeValueAsString(authPostPO);
                                WinterExcelBusinessErrorModel errorModel =
                                        WinterExcelBusinessErrorModel.builder()
                                                .errorMessage(winterI18nTemplate.message(CommonConstants.I18nKey.POST_NAME_OR_IDENTIFIER_EXISTS))
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
                                 return authPostMpService.save(authPostPO);
                            });
                        }
                    }
                }, fastFalseValidator, CollUtil.toList(AuthPostPO.Import.class));

        // ====================== 4. 执行 Excel 读取 ======================
        FastExcel.read(file.getInputStream(), AuthPostPO.class, analysisValidReadListener)
                .excelType(ExcelTypeEnum.XLSX)
                .password("")
                .sheet(0)
                .doRead();

        // ====================== 5. 收集校验错误信息 ======================
        // JSR-303 校验错误
        List<WinterExcelValidateErrorModel> errorList =
                analysisValidReadListener.getErrorList();

        // ====================== 6. 构建业务逻辑错误 Sheet ======================
        if (!ObjectUtils.isEmpty(winterExcelBusinessErrorModelList)) {

            WinterExcelExportParam<WinterExcelBusinessErrorModel> businessParam =
                    WinterExcelExportParam.<WinterExcelBusinessErrorModel>builder()
                            .sheetName(winterI18nTemplate.message(CommonConstants.I18nKey.BUSINESS_LOGIC_ERROR))
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
                        stringBuilder.append(winterI18nTemplate.message(messageKey, new Object[]{}, message));
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
                            .sheetName(winterI18nTemplate.message(CommonConstants.I18nKey.VALIDATION_LOGIC_ERROR))
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
            Response<Object> build = Response.build(null, ResultCodeEnum.SUCCESS.getCode(), winterI18nTemplate.message(CommonConstants.I18nKey.IMPORT_SUCCESSFUL));
            WebUtil.renderString(response, objectMapper.writeValueAsString(build));

        } else {
            // 存在错误，导出多 Sheet 的错误 Excel
            winterExcelTemplate.exportMultiSheet(
                    response,
                    winterI18nTemplate.message(CommonConstants.I18nKey.ERROR_MESSAGE)+".xlsx",
                    "",
                    excelExportParamList
            );
        }
    }

    /**
     * 导出职位Excel数据
     * <p>
     * 将职位列表导出为Excel文件，状态字段会转换为字典显示值
     *
     * @param response HTTP响应对象，用于输出Excel文件
     * @param records 职位领域对象列表
     */
    @Override
    public void postExportExcel(HttpServletResponse response, List<AuthPostDO> records) {
        // 状态
        Map<String, String> statusMap = dictCache("110", true);
        List<AuthPostPO> poList = authPostInfraAssembler.toPOList(records);
        List<AuthPostPO> collect = poList.stream()
                .map(item -> {
                    item.setStatus(statusMap.getOrDefault(item.getStatus(),""));
                    return item;
                }).collect(Collectors.toList());
        ArrayList<WriteHandler> writeHandlers = new ArrayList<>();
        // 自定义样式处理器
        CustomStyleHandler cellStyleSheetWriteHandler = new CustomStyleHandler(null, null);
        writeHandlers.add(cellStyleSheetWriteHandler);
        writeHandlers.add(new CustomMatchColumnWidthStyleHandler());
        WinterExcelExportParam<AuthPostPO> builder = WinterExcelExportParam.<AuthPostPO>builder()
                .response(response)
                .batchSize(1000)
                .password("")
                .fileName(winterI18nTemplate.message(CommonConstants.I18nKey.POST_INFORMATION)+".xlsx")
                .excludeColumnFieldNames(null)
                .writeHandlers(writeHandlers)
                .converters(null)
                .head(AuthPostPO.class)
                .dataList(collect)
                .build();
        winterExcelTemplate.export(builder);
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
