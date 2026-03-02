package com.winter.cloud.auth.infrastructure.repository;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapBuilder;
import cn.hutool.core.util.ObjectUtil;
import cn.idev.excel.write.handler.WriteHandler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winter.cloud.auth.api.dto.command.UserRegisterCommand;
import com.winter.cloud.auth.api.dto.query.UserQuery;
import com.winter.cloud.auth.api.dto.response.DeptResponseDTO;
import com.winter.cloud.auth.api.dto.response.RoleResponseDTO;
import com.winter.cloud.auth.api.dto.response.UserResponseDTO;
import com.winter.cloud.auth.domain.model.entity.AuthUserDO;
import com.winter.cloud.auth.domain.repository.AuthUserRepository;
import com.winter.cloud.auth.infrastructure.assembler.AuthUserInfraAssembler;
import com.winter.cloud.auth.infrastructure.entity.*;
import com.winter.cloud.auth.infrastructure.mapper.AuthUserMapper;
import com.winter.cloud.auth.infrastructure.service.IAuthPostMPService;
import com.winter.cloud.auth.infrastructure.service.IAuthUserDeptMpService;
import com.winter.cloud.auth.infrastructure.service.IAuthUserMpService;
import com.winter.cloud.auth.infrastructure.service.IAuthUserRoleMpService;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.common.exception.BusinessException;
import com.winter.cloud.common.response.PageDTO;
import com.winter.cloud.common.response.Response;
import com.winter.cloud.dict.api.dto.command.DictCommand;
import com.winter.cloud.dict.api.dto.query.DictQuery;
import com.winter.cloud.dict.api.dto.response.DictDataDTO;
import com.winter.cloud.dict.api.facade.DictFacade;
import com.zsq.i18n.template.WinterI18nTemplate;
import com.zsq.winter.encrypt.util.CryptoUtil;
import com.zsq.winter.office.entity.excel.WinterExcelExportParam;
import com.zsq.winter.office.entity.excel.WinterExcelSelectedModel;
import com.zsq.winter.office.entity.excel.handler.CustomMatchColumnWidthStyleHandler;
import com.zsq.winter.office.entity.excel.handler.CustomSelectHandler;
import com.zsq.winter.office.entity.excel.handler.CustomStyleHandler;
import com.zsq.winter.office.service.excel.WinterExcelTemplate;
import com.zsq.winter.redis.ddc.service.WinterRedisTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Validator;
import java.util.*;
import java.util.stream.Collectors;

import static com.winter.cloud.common.enums.ResultCodeEnum.DUPLICATE_KEY;
import static com.winter.cloud.common.enums.ResultCodeEnum.FAIL;
@Slf4j
@Repository
@RequiredArgsConstructor
public class AuthUserRepositoryImpl implements AuthUserRepository {
    private final IAuthUserMpService authUserMpService;
    private final IAuthUserRoleMpService authUserRoleMpService;
    private final IAuthUserDeptMpService authUserDeptMpService;
    private final IAuthPostMPService authPostMpService;
    private final AuthUserMapper authUserMapper;
    private final AuthUserInfraAssembler authUserInfraAssembler;
    private final WinterI18nTemplate winterI18nTemplate;
    private final WinterExcelTemplate winterExcelTemplate;
    private final WinterRedisTemplate winterRedisTemplate;
    private final ObjectMapper objectMapper;
    private final Validator fastFalseValidator;
    @DubboReference(check = false)
    private DictFacade dictFacade;
    @Override
    public AuthUserDO findById(Long id) {
        return null;
    }

    @Override
    public AuthUserDO findByEmail(String email) {
        AuthUserPO po = authUserMpService.getOne(new LambdaQueryWrapper<AuthUserPO>().eq(AuthUserPO::getEmail, email));
        return authUserInfraAssembler.toDO(po);
    }

    @Override
    public Boolean save(AuthUserDO authUserDo) {
        AuthUserPO po = authUserInfraAssembler.toPO(authUserDo);
        return authUserMpService.save(po);
    }

    @Override
    public List<String> getRoleKeyList(Long userId) {
        return authUserMapper.getRoleKeyList(userId);
    }


    @Override
    public void deleteById(Long id) {

    }

    /**
     * 检查用户是否存在
     * 注册时，肯定是没有用户id的（后续也可以接入valid注解校验），更新用户信息时，肯定要传入用户id,更新判断需要排除自身
     * 只要库里有任何人占用了这三个信息（username,phone,email）中的任意一个，就算重复
     *
     * @param command 注册命令
     * @return 是否存在
     */
    @Override
    public boolean hasDuplicateUser(UserRegisterCommand command) {
        LambdaQueryWrapper<AuthUserPO> authUserPOLambdaQueryWrapper = new LambdaQueryWrapper<>();
        authUserPOLambdaQueryWrapper.nested(
                        e -> e.eq(AuthUserPO::getUserName, command.getUserName())
                                .or()
                                .eq(AuthUserPO::getPhone, command.getPhone())
                                .or()
                                .eq(AuthUserPO::getEmail, command.getEmail()))
                .ne(ObjectUtil.isNotEmpty(command.getId()), AuthUserPO::getId, command.getId());
        long count = authUserMpService.count(authUserPOLambdaQueryWrapper);
        return count > 0;
    }


    @Override
    public PageDTO<AuthUserDO> userPage(UserQuery userQuery) {
        // 1. 构建分页对象
        Page<AuthUserPO> page = new Page<>(userQuery.getPageNum(), userQuery.getPageSize());

        // 2. 调用 Mapper 执行自定义的复杂 SQL 查询
        // 注意：这里不能简单使用 MyBatis-Plus 的 Wrapper，因为涉及"同时拥有多个角色/部门"的 HAVING 逻辑
        IPage<AuthUserPO> userPage = authUserMapper.selectUserPage(page, userQuery);
        List<AuthUserDO> doList = authUserInfraAssembler.toDOList(userPage.getRecords());
        return new PageDTO<>(doList, userPage.getTotal());
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean userSave(AuthUserDO aDo) {

        if (this.hasDuplicateUser(authUserInfraAssembler.toUserRegisterCommand(aDo))) {
            throw new BusinessException(DUPLICATE_KEY.getCode(), winterI18nTemplate.message(CommonConstants.I18nKey.USER_INFO_DUPLICATED));
        }

        AuthUserPO authUserPO = authUserInfraAssembler.toPO(aDo);
        String encryptedPwd = CryptoUtil.winterMd5Hex16(authUserPO.getPassword());
        authUserPO.setPassword(encryptedPwd);
        boolean saved = authUserMpService.save(authUserPO);
        if (!saved) {
            throw new BusinessException(FAIL.getCode(), winterI18nTemplate.message(CommonConstants.I18nKey.USER_INFO_SAVE_FAILED));
        }

        Long userId = authUserPO.getId();

        List<AuthUserRolePO> roleList =
                Optional.ofNullable(aDo.getRoleIds())
                        .orElse(List.of())
                        .stream()
                        .map(item -> AuthUserRolePO.builder()
                                .userId(userId)
                                .roleId(item)
                                .build())
                        .collect(Collectors.toList());

        if (!roleList.isEmpty() && !authUserRoleMpService.saveBatch(roleList)) {
            throw new BusinessException(FAIL.getCode(), winterI18nTemplate.message(CommonConstants.I18nKey.USER_ROLE_SAVE_FAILED));
        }

        List<AuthUserDeptPO> deptList =
                Optional.ofNullable(aDo.getDeptIds())
                        .orElse(List.of())
                        .stream()
                        .map(item -> AuthUserDeptPO.builder()
                                .userId(userId)
                                .deptId(item)
                                .build())
                        .collect(Collectors.toList());

        if (!deptList.isEmpty() && !authUserDeptMpService.saveBatch(deptList)) {
            throw new BusinessException(FAIL.getCode(), winterI18nTemplate.message(CommonConstants.I18nKey.USER_DEPT_SAVE_FAILED));
        }
        return true;
    }

    /**
     * 更新用户信息
     * 该方法是一个完整的事务方法，包含以下操作：
     * 1. 检查用户信息是否重复（用户名、手机号、邮箱）
     * 2. 更新用户基本信息
     * 3. 重新维护用户角色关联关系（先删后增）
     * 4. 重新维护用户部门关联关系（先删后增）
     *
     * @param aDo 用户领域实体，包含更新后的用户信息
     * @return 更新成功返回 true
     * @throws BusinessException 当发生以下情况时抛出业务异常：
     *                           - 用户信息重复（用户名/手机号/邮箱已被其他用户使用）
     *                           - 用户更新失败
     *                           - 用户角色关联保存失败
     *                           - 用户部门关联保存失败
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean userUpdate(AuthUserDO aDo) {
        /**
         * 步骤1：检查用户信息是否重复
         * 使用 hasDuplicateUser 方法进行检查，该方法会：
         * - 检查用户名、手机号、邮箱是否与现有用户重复
         * - 排除当前用户自身（通过 ne 条件），避免更新时误报重复
         *
         * 原理：hasDuplicateUser 方法内部使用 .ne() 条件，当 command.getId() 不为空时，
         * 会排除该 ID 的记录，这样更新时就不会把当前用户的信息判断为重复
         */
        if (this.hasDuplicateUser(authUserInfraAssembler.toUserRegisterCommand(aDo))) {
            throw new BusinessException(DUPLICATE_KEY.getCode(), winterI18nTemplate.message(CommonConstants.I18nKey.USER_INFO_DUPLICATED));
        }

        /**
         * 步骤2：更新用户基本信息
         * 将领域实体转换为持久化实体，然后调用 MyBatis-Plus 的 updateById 方法进行更新
         * 注意：这里直接更新整个实体，如果某些字段为 null，可能会被设置为 null
         *       建议在业务层或转换层处理 null 值的保留逻辑
         */
        AuthUserPO authUserPO = authUserInfraAssembler.toPO(aDo);
        boolean updated = authUserMpService.updateById(authUserPO);
        if (!updated) {
            throw new BusinessException(FAIL.getCode(), winterI18nTemplate.message(CommonConstants.I18nKey.USER_INFO_UPDATE_FAILED));
        }

        /**
         * 步骤3：维护用户角色关联关系
         * 采用"先删后增"的策略：
         * 1. 先删除该用户的所有现有角色关联
         * 2. 再批量插入新的角色关联
         *
         * 这种方式的优点：
         * - 实现简单，不需要对比差异
         * - 可以处理角色被全部取消的情况
         *
         * 缺点：
         * - 如果角色很多，删除和插入操作会有一定性能开销
         * - 如果中间步骤失败，需要注意事务回滚
         */
        Long userId = authUserPO.getId();

        // 3.1 删除用户角色关联
        // 使用 MyBatis-Plus 的 LambdaQueryWrapper 构建删除条件
        authUserRoleMpService.remove(new LambdaQueryWrapper<AuthUserRolePO>().eq(AuthUserRolePO::getUserId, userId));

        // 3.2 构建角色关联实体列表
        // 使用 Optional 处理可能的 null 情况，优雅地处理空列表
        List<AuthUserRolePO> roleList =
                Optional.ofNullable(aDo.getRoleIds())
                        .orElse(List.of())  // 如果 roleIds 为 null，则使用空列表
                        .stream()
                        .map(item -> AuthUserRolePO.builder()
                                .userId(userId)     // 设置用户ID
                                .roleId(item)       // 设置角色ID
                                .build())
                        .collect(Collectors.toList());

        // 3.3 批量保存角色关联
        // 只有当角色列表不为空时才执行保存操作
        boolean b = authUserRoleMpService.saveBatch(roleList);
        if (!roleList.isEmpty() && !b) {
            throw new BusinessException(FAIL.getCode(), winterI18nTemplate.message(CommonConstants.I18nKey.USER_ROLE_UPDATE_FAILED));
        }

        /**
         * 步骤4：维护用户部门关联关系
         * 与步骤3相同的"先删后增"策略：
         * 1. 先删除该用户的所有现有部门关联
         * 2. 再批量插入新的部门关联
         *
         * 注意：用户角色和用户部门是两个独立的关联表，需要分别维护
         */
        // 4.1 删除用户部门关联
        authUserDeptMpService.remove(new LambdaQueryWrapper<AuthUserDeptPO>().eq(AuthUserDeptPO::getUserId, userId));

        // 4.2 构建部门关联实体列表
        List<AuthUserDeptPO> deptList =
                Optional.ofNullable(aDo.getDeptIds())
                        .orElse(List.of())  // 如果 deptIds 为 null，则使用空列表
                        .stream()
                        .map(item -> AuthUserDeptPO.builder()
                                .userId(userId)   // 设置用户ID
                                .deptId(item)     // 设置部门ID
                                .build())
                        .collect(Collectors.toList());

        // 4.3 批量保存部门关联
        boolean c = authUserDeptMpService.saveBatch(deptList);
        if (!deptList.isEmpty() && !c) {
            throw new BusinessException(FAIL.getCode(), winterI18nTemplate.message(CommonConstants.I18nKey.USER_DEPT_UPDATE_FAILED));
        }

        /**
         * 返回更新成功标识
         * 由于整个方法在事务中执行，如果任何步骤失败都会抛出异常并回滚
         * 所以能执行到这里说明所有操作都成功了
         */
        return true;
    }

    /**
     * 批量删除用户及其关联数据
     * 该方法是一个完整的事务方法，执行以下操作：
     * 1. 删除用户角色关联信息
     * 2. 删除用户部门关联信息
     * 3. 删除用户基本信息
     * <p>
     * 【重要】删除顺序说明：
     * 必须先删除关联数据（角色、部门），最后删除用户信息
     * 原因：如果先删除用户，关联数据将无法通过 userId 查询到，导致关联数据残留
     *
     * @param idList 用户ID列表，用于批量删除
     * @return 删除成功返回 true，删除失败或参数为空返回 false
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean userDelete(List<Long> idList) {
        // 参数校验：检查用户ID列表是否为空
        if (ObjectUtil.isEmpty(idList)) {
            return false;
        }

        /**
         * 步骤1：删除用户角色关联信息
         * 使用 LambdaQueryWrapper 构建删除条件，根据用户ID列表删除关联数据
         * 注意：这里直接根据 userId 删除，不需要先查询再删除，效率更高
         */
        boolean userRoleRemoved = authUserRoleMpService.remove(
                new LambdaQueryWrapper<AuthUserRolePO>()
                        .in(AuthUserRolePO::getUserId, idList)
        );

        /**
         * 步骤2：删除用户部门关联信息
         * 与步骤1相同的删除策略，直接根据 userId 删除
         */
        boolean userDeptRemoved = authUserDeptMpService.remove(
                new LambdaQueryWrapper<AuthUserDeptPO>()
                        .in(AuthUserDeptPO::getUserId, idList)
        );

        /**
         * 步骤3：删除用户基本信息
         * 最后删除用户本身，此时关联数据已经清理干净，不会出现外键约束问题
         * removeBatchByIds 方法会根据主键批量删除用户记录
         */
        boolean userRemoved = authUserMpService.removeBatchByIds(idList);


        return true;
    }

    @Override
    public Response<Boolean> updatePasswordBySuperMan(Long id, String password) {
        boolean update = authUserMpService.update(new LambdaUpdateWrapper<AuthUserPO>().set(AuthUserPO::getPassword, CryptoUtil.winterMd5Hex16(password)).eq(AuthUserPO::getId, id));
        return Response.ok(update);
    }


    @Override
    public void userExportExcelTemplate(HttpServletResponse response) {
        String[] postArr = authPostMpService.list(new LambdaQueryWrapper<AuthPostPO>().select(AuthPostPO::getPostName)).stream().map(AuthPostPO::getPostName).toArray(String[]::new);
        Map<Integer, WinterExcelSelectedModel> selectedModelHashMap = new HashMap<>();
        selectedModelHashMap.put(9, WinterExcelSelectedModel
                .builder()
                .firstRow(2)
                .lastRow(5000)
                .source(postArr)
                .build());

        ArrayList<WriteHandler> writeHandlers = new ArrayList<>();

        // 自定义职位下拉框数据，因为不是单独的字典数据
        CustomSelectHandler customSelectHandler = new CustomSelectHandler(selectedModelHashMap);
        // 自定义样式处理器
        CustomStyleHandler cellStyleSheetWriteHandler = new CustomStyleHandler(null, null);
        writeHandlers.add(cellStyleSheetWriteHandler);
        writeHandlers.add(customSelectHandler);
        writeHandlers.add(new CustomMatchColumnWidthStyleHandler());
        WinterExcelExportParam<AuthUserPO> builder = WinterExcelExportParam.<AuthUserPO>builder()
                .response(response)
                .batchSize(1000)
                .password("")
                .fileName(winterI18nTemplate.message(CommonConstants.I18nKey.USER_INFORMATION_TEMPLATE) + ".xlsx")
                // 导出的模版不需要创建时间这个列
                .excludeColumnFieldNames(List.of("createTime"))
                .converters(null)
                .writeHandlers(writeHandlers)
                .head(AuthUserPO.class)
                .dataList(null)
                .build();
        winterExcelTemplate.export(builder);
    }

    @Override
    public void userImportExcel(HttpServletResponse response, MultipartFile file) {

    }

    @Override
    public void userExportExcel(HttpServletResponse response, List<UserResponseDTO> records) {
        Response<List<DictDataDTO>> statusListResponse = dictFacade.dictValueDynamicQueryList(DictQuery.builder().dictTypeId(110L).build());
        List<DictDataDTO> statusList = statusListResponse.getData();
        //映射
        Map<String, String> statusMap = statusList.stream().collect(Collectors.toMap(DictDataDTO::getDictValue, DictDataDTO::getDictLabel));

        Response<List<DictDataDTO>> sexListResponse = dictFacade.dictValueDynamicQueryList(DictQuery.builder().dictTypeId(1L).build());
        List<DictDataDTO> sexList = sexListResponse.getData();
        //映射
        Map<String, String> sexMap = sexList.stream().collect(Collectors.toMap(DictDataDTO::getDictValue, DictDataDTO::getDictLabel));

        // 构建多级别表头
        Map<String, List<String>> headMap = new LinkedHashMap<>();
        headMap.put("userName", List.of("用户信息","用户名称"));
        headMap.put("nickName", List.of("用户信息","用户昵称"));
        headMap.put("email", List.of("用户信息","用户邮箱"));
        headMap.put("phone", List.of("用户信息","手机号码"));
        headMap.put("status", List.of("用户信息","用户状态"));
        headMap.put("sex", List.of("用户信息","性别"));
        headMap.put("avatar", List.of("用户信息","头像地址"));
        headMap.put("remark", List.of("用户信息","用户备注"));
        headMap.put("postName", List.of("用户信息","职位名称"));
        headMap.put("deptName", List.of("用户信息","部门名称"));
        headMap.put("roleName", List.of("用户信息","角色名称"));
        // 构建数据
        List<Map<String, Object>> mapDataList = records.stream()
                .map((item) -> {
                    String status = statusMap.get(item.getStatus());
                    String sex = sexMap.get(item.getSex());
                    String postName = item.getPostDTO().getPostName();
                    String deptName = item.getDeptListDTO().stream().map(DeptResponseDTO::getDeptName).collect(Collectors.joining(","));
                    String roleName = item.getRoleListDTO().stream().map(RoleResponseDTO::getRoleName).collect(Collectors.joining(","));
                    return MapBuilder
                            .create(new HashMap<String, Object>())
                            .put("userName", item.getUserName())
                            .put("nickName", item.getNickName())
                            .put("email", item.getEmail())
                            .put("phone", item.getPhone())
                            .put("status", status)
                            .put("sex", sex)
                            .put("avatar", item.getAvatar())
                            .put("remark", item.getRemark())
                            .put("postName", postName)
                            .put("deptName", deptName)
                            .put("roleName", roleName)
                            .map();
                }).collect(Collectors.toList());
        ArrayList<WriteHandler> writeHandlers = new ArrayList<>();
        // 自定义样式处理器
        CustomStyleHandler cellStyleSheetWriteHandler = new CustomStyleHandler(null, null);
        writeHandlers.add(cellStyleSheetWriteHandler);
        writeHandlers.add(new CustomMatchColumnWidthStyleHandler());
        WinterExcelExportParam<AuthUserPO> builder = WinterExcelExportParam.<AuthUserPO>builder()
                .response(response)
                .batchSize(1000)
                .password("")
                .fileName(winterI18nTemplate.message(CommonConstants.I18nKey.USER_INFORMATION)+".xlsx")
                .excludeColumnFieldNames(null)
                .writeHandlers(writeHandlers)
                .converters(null)
                .headColumnMap(headMap)
                .mapDataList(mapDataList)
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
