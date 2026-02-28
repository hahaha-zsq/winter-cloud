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
import com.winter.cloud.auth.api.dto.query.RoleQuery;
import com.winter.cloud.auth.api.dto.response.RoleResponseDTO;
import com.winter.cloud.auth.domain.model.entity.AuthRoleDO;
import com.winter.cloud.auth.domain.repository.AuthRoleRepository;
import com.winter.cloud.auth.infrastructure.assembler.AuthRoleInfraAssembler;
import com.winter.cloud.auth.infrastructure.entity.AuthRoleMenuPO;
import com.winter.cloud.auth.infrastructure.entity.AuthRolePO;
import com.winter.cloud.auth.infrastructure.entity.AuthUserPO;
import com.winter.cloud.auth.infrastructure.entity.AuthUserRolePO;
import com.winter.cloud.auth.infrastructure.mapper.AuthRoleMapper;
import com.winter.cloud.auth.infrastructure.service.IAuthRoleMPService;
import com.winter.cloud.auth.infrastructure.service.IAuthRoleMenuMpService;
import com.winter.cloud.auth.infrastructure.service.IAuthUserRoleMpService;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.exception.BusinessException;
import com.winter.cloud.common.response.PageDTO;
import com.winter.cloud.common.response.Response;
import com.winter.cloud.dict.api.dto.command.DictCommand;
import com.winter.cloud.dict.api.dto.query.DictQuery;
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

@Slf4j
@Repository
@RequiredArgsConstructor
public class AuthRoleRepositoryImpl implements AuthRoleRepository {
    private final IAuthUserRoleMpService authUserRoleMpService;
    private final IAuthRoleMPService authRoleMpService;
    private final IAuthRoleMenuMpService authRoleMenuMpService;
    private final AuthRoleMapper authRoleMapper;
    private final AuthRoleInfraAssembler authRoleInfraAssembler;
    private final WinterI18nTemplate winterI18nTemplate;
    private final WinterExcelTemplate winterExcelTemplate;
    private final WinterRedisTemplate winterRedisTemplate;
    private final ObjectMapper objectMapper;
    private final Validator fastFalseValidator;
    /**
     * 编程式事务模版
     * <p>
     * 用于精确控制事务边界。解决 @Transactional 注解与缓存操作的时序问题。
     * 如果使用注解，缓存操作通常包裹在事务内，若缓存更新成功但后续 DB 提交失败，会导致缓存脏数据。
     */
    private final TransactionTemplate transactionTemplate;
    @DubboReference(check = false)
    private DictFacade dictFacade;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean roleSave(AuthRoleDO authRoleDO) {
        boolean b = this.hasDuplicateRole(authRoleDO);
        if (b) {
            throw new BusinessException(DUPLICATE_KEY.getCode(), winterI18nTemplate.message(CommonConstants.I18nKey.ROLE_NAME_OR_IDENTIFIER_EXISTS));
        }
        AuthRolePO authRolePO = authRoleInfraAssembler.toPO(authRoleDO);
        return authRoleMpService.save(authRolePO);
    }

    /**
     * 判断是否存在：角色名相同 或 角色编码相同并且（如果是更新）不是自己
     */
    @Override
    public boolean hasDuplicateRole(AuthRoleDO authRoleDO) {
        LambdaQueryWrapper<AuthRolePO> authRolePOLambdaQueryWrapper = new LambdaQueryWrapper<>();
        authRolePOLambdaQueryWrapper.nested(
                        e -> e.eq(AuthRolePO::getRoleName, authRoleDO.getRoleName())
                                .or()
                                .eq(AuthRolePO::getRoleKey, authRoleDO.getRoleKey()))
                .ne(ObjectUtil.isNotEmpty(authRoleDO.getId()), AuthRolePO::getId, authRoleDO.getId());
        long count = authRoleMpService.count(authRolePOLambdaQueryWrapper);
        return count > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean roleUpdate(AuthRoleDO authRoleDO) {
        boolean b = this.hasDuplicateRole(authRoleDO);
        if (b) {
            throw new BusinessException(DUPLICATE_KEY.getCode(), winterI18nTemplate.message(CommonConstants.I18nKey.ROLE_NAME_OR_IDENTIFIER_EXISTS));
        }
        AuthRolePO authRolePO = authRoleInfraAssembler.toPO(authRoleDO);
        return authRoleMpService.updateById(authRolePO);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean roleDelete(List<Long> roleIds) {
        if (ObjectUtil.isEmpty(roleIds)) {
            return false;
        }

        List<Long> allowDeleteRoleIds;
        // 查询被用户关联的角色
        List<AuthUserRolePO> list = authUserRoleMpService.list(
                new LambdaQueryWrapper<AuthUserRolePO>().in(AuthUserRolePO::getRoleId, roleIds)
        );

        if (ObjectUtil.isNotEmpty(list)) {
            List<Long> collect = list.stream().map(AuthUserRolePO::getRoleId).collect(Collectors.toList());
            allowDeleteRoleIds = roleIds.stream().filter(roleId -> !collect.contains(roleId)).collect(Collectors.toList());
        } else {
            allowDeleteRoleIds = roleIds;
        }

        // 1. 拦截空集合：如果没有可以删除的角色，直接中断处理
        if (ObjectUtil.isEmpty(allowDeleteRoleIds)) {
            // 建议在这里抛出自定义业务异常，提示前端 "所选角色均已关联用户，无法删除"
            throw new BusinessException(ResultCodeEnum.FAIL_LANG.getCode(), winterI18nTemplate.message("Roles.cannot.delete"));
        }

        // 2. 执行主表删除
        boolean b = authRoleMpService.removeByIds(allowDeleteRoleIds);
        if (!b) {
            // 主表删除失败，必须抛出异常触发回滚！
            throw new BusinessException(ResultCodeEnum.FAIL_LANG.getCode(), winterI18nTemplate.message("delete.fail")); // 替换为你项目中的实际业务异常类
        }

        // 3. 执行关联表删除
        // 注意：这里不需要接收返回值，也不需要判断 true/false。
        // 因为有没有关联菜单都可以，影响行数为 0 (返回false) 也是正常的业务场景。
        authRoleMenuMpService.remove(
                new LambdaQueryWrapper<AuthRoleMenuPO>().in(AuthRoleMenuPO::getRoleId, allowDeleteRoleIds)
        );
        return true;
    }

    /**
     * 分页查询角色列表，并支持安全的字段排序（使用 Stream 流处理）。
     *
     * @param roleQuery 查询条件（包含分页参数、排序规则、搜索关键字等）
     * @return 分页结果，封装为 {@link PageDTO<RoleResponseDTO>}
     */
    @Override
    public PageDTO<AuthRoleDO> rolePage(RoleQuery roleQuery) {
        // 1. 构建分页对象
        Page<AuthUserPO> page = new Page<>(roleQuery.getPageNum(), roleQuery.getPageSize());
        IPage<AuthRolePO> rolePage = authRoleMapper.selectRolePage(page, roleQuery);
        List<AuthRoleDO> doList = authRoleInfraAssembler.toDOList(rolePage.getRecords());
        return new PageDTO<>(doList, rolePage.getTotal());
    }


    @Override
    public List<AuthRoleDO> selectRoleListByUserId(Long userId, String status) {
        if (ObjectUtil.isNotEmpty(userId)) {
            List<AuthRolePO> authRolePOList = authRoleMapper.selectRoleIdListByUserId(userId, status);
            return authRoleInfraAssembler.toDOList(authRolePOList);
        }
        return List.of();
    }

    @Override
    public List<AuthRoleDO> roleDynamicQueryList(RoleQuery roleQuery) {
        LambdaQueryWrapper<AuthRolePO> queryWrapper = new LambdaQueryWrapper<AuthRolePO>()
                .eq(ObjectUtil.isNotEmpty(roleQuery.getId()), AuthRolePO::getId, roleQuery.getId())
                .eq(ObjectUtil.isNotEmpty(roleQuery.getStatus()), AuthRolePO::getStatus, roleQuery.getStatus())
                .like(ObjectUtil.isNotEmpty(roleQuery.getRoleKey()), AuthRolePO::getRoleKey, roleQuery.getRoleKey())
                .like(ObjectUtil.isNotEmpty(roleQuery.getRoleName()), AuthRolePO::getRoleName, roleQuery.getRoleName());
        List<AuthRolePO> list = authRoleMpService.list(queryWrapper);
        return authRoleInfraAssembler.toDOList(list);
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void assignMenuPermissions(Long roleId, List<Long> menuIds) {
        //  删除之前角色拥有的资源
        authRoleMenuMpService.remove(new LambdaQueryWrapper<AuthRoleMenuPO>().eq(AuthRoleMenuPO::getRoleId, roleId));
        //  添加角色新的资源
        List<AuthRoleMenuPO> authRoleMenuPOList = menuIds.stream().map(menuId -> AuthRoleMenuPO.builder().roleId(roleId).menuId(menuId).build()).collect(Collectors.toList());
        authRoleMenuMpService.saveBatch(authRoleMenuPOList, 100);
    }

    @Override
    public void roleExportExcel(HttpServletResponse response) {
        // 状态
        Response<List<DictDataDTO>> listLocaleResponse = dictFacade.dictValueDynamicQueryList(DictQuery.builder().dictTypeId(110L).build());
        List<DictDataDTO> statuList = listLocaleResponse.getData();
        // 语言环境映射
        Map<String, String> statusMap = statuList.stream().collect(Collectors.toMap(DictDataDTO::getDictValue, DictDataDTO::getDictLabel));
        List<AuthRolePO> collect = authRoleMpService.list().stream()
                .map(item -> {
                    item.setStatus(statusMap.get(item.getStatus()));
                    return item;
                }).collect(Collectors.toList());
        ArrayList<WriteHandler> writeHandlers = new ArrayList<>();
        // 自定义样式处理器
        CustomStyleHandler cellStyleSheetWriteHandler = new CustomStyleHandler(null, null);
        writeHandlers.add(cellStyleSheetWriteHandler);
        writeHandlers.add(new CustomMatchColumnWidthStyleHandler());
        WinterExcelExportParam<AuthRolePO> builder = WinterExcelExportParam.<AuthRolePO>builder()
                .response(response)
                .batchSize(1000)
                .password("")
                .fileName(winterI18nTemplate.message(CommonConstants.I18nKey.ROLE_INFORMATION)+".xlsx")
                .excludeColumnFieldNames(null)
                .writeHandlers(writeHandlers)
                .converters(null)
                .head(AuthRolePO.class)
                .dataList(collect)
                .build();
        winterExcelTemplate.export(builder);
    }

    @Override
    public void roleExportExcelTemplate(HttpServletResponse response) {
        ArrayList<WriteHandler> writeHandlers = new ArrayList<>();
        // 自定义样式处理器
        CustomStyleHandler cellStyleSheetWriteHandler = new CustomStyleHandler(null, null);
        writeHandlers.add(cellStyleSheetWriteHandler);
        writeHandlers.add(new CustomMatchColumnWidthStyleHandler());
        WinterExcelExportParam<AuthRolePO> builder = WinterExcelExportParam.<AuthRolePO>builder()
                .response(response)
                .batchSize(1000)
                .password("")
                .fileName(winterI18nTemplate.message(CommonConstants.I18nKey.ROLE_INFORMATION_TEMPLATE)+".xlsx")
                // 导出的模版不需要创建时间这个列
                .excludeColumnFieldNames(List.of("createTime"))
                .converters(null)
                .writeHandlers(writeHandlers)
                .head(AuthRolePO.class)
                .dataList(null)
                .build();
        winterExcelTemplate.export(builder);
    }

    @Override
    public void roleImportExcel(HttpServletResponse response, MultipartFile file) throws IOException {

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
        WinterAnalysisValidReadListener<AuthRolePO> i18nMessagePOAnalysisValidReadListener =
                new WinterAnalysisValidReadListener<>(1000, (item) -> {
                    // ====================== 3. 处理每一批校验通过的数据 ======================
                    for (AuthRolePO authRolePO : item) {
                        // 将 Excel 中的字典值转换为系统内部值
                        String statusOrDefault = statusMap.getOrDefault(authRolePO.getStatus(), "");
                        if (!ObjectUtil.isEmpty(statusOrDefault)) {
                            authRolePO.setStatus(statusOrDefault);
                        } else {
                            String jsonStr = null;
                            try {
                                jsonStr = objectMapper.writeValueAsString(authRolePO);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                            WinterExcelBusinessErrorModel errorModel =
                                    WinterExcelBusinessErrorModel.builder()
                                            .errorMessage(winterI18nTemplate.message(CommonConstants.I18nKey.ROLE_DICT_MAPPING_ERROR))
                                            .entityRowInfo(jsonStr)
                                            .build();

                            winterExcelBusinessErrorModelList.add(errorModel);
                        }
                        // 校验是否已存在（唯一性校验）
                        boolean hasDuplicate = hasDuplicateRole(
                                AuthRoleDO.builder()
                                .roleKey(authRolePO.getRoleKey())
                                .roleName(authRolePO.getRoleName())
                                .status(authRolePO.getStatus())
                                .id(authRolePO.getId())
                                .build());

                        if (hasDuplicate) {
                            // ====================== 3.1 业务唯一性校验失败 ======================
                            try {
                                // 将当前行数据序列化，方便导出错误信息
                                String jsonStr = objectMapper.writeValueAsString(authRolePO);
                                WinterExcelBusinessErrorModel errorModel =
                                        WinterExcelBusinessErrorModel.builder()
                                                .errorMessage(winterI18nTemplate.message(CommonConstants.I18nKey.ROLE_NAME_OR_IDENTIFIER_EXISTS))
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
                                 return authRoleMpService.save(authRolePO);
                            });
                        }
                    }
                }, fastFalseValidator, CollUtil.toList(AuthRolePO.Import.class));

        // ====================== 4. 执行 Excel 读取 ======================
        FastExcel.read(file.getInputStream(), AuthRolePO.class, i18nMessagePOAnalysisValidReadListener)
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
