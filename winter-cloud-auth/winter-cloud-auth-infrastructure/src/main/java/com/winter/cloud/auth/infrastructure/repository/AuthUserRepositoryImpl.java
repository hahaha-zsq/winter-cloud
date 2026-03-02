package com.winter.cloud.auth.infrastructure.repository;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.winter.cloud.auth.api.dto.command.UserRegisterCommand;
import com.winter.cloud.auth.api.dto.query.UserQuery;
import com.winter.cloud.auth.domain.model.entity.AuthUserDO;
import com.winter.cloud.auth.domain.repository.AuthUserRepository;
import com.winter.cloud.auth.infrastructure.assembler.AuthUserInfraAssembler;
import com.winter.cloud.auth.infrastructure.entity.AuthRolePO;
import com.winter.cloud.auth.infrastructure.entity.AuthUserDeptPO;
import com.winter.cloud.auth.infrastructure.entity.AuthUserPO;
import com.winter.cloud.auth.infrastructure.entity.AuthUserRolePO;
import com.winter.cloud.auth.infrastructure.mapper.AuthUserMapper;
import com.winter.cloud.auth.infrastructure.service.IAuthUserDeptMpService;
import com.winter.cloud.auth.infrastructure.service.IAuthUserMpService;
import com.winter.cloud.auth.infrastructure.service.IAuthUserRoleMpService;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.exception.BusinessException;
import com.winter.cloud.common.response.PageDTO;
import com.winter.cloud.common.response.Response;
import com.zsq.i18n.template.WinterI18nTemplate;
import com.zsq.winter.encrypt.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.winter.cloud.common.enums.ResultCodeEnum.DUPLICATE_KEY;
import static com.winter.cloud.common.enums.ResultCodeEnum.FAIL;

@Repository
@RequiredArgsConstructor
public class AuthUserRepositoryImpl implements AuthUserRepository {
    private final IAuthUserMpService authUserMpService;
    private final IAuthUserRoleMpService authUserRoleMpService;
    private final IAuthUserDeptMpService authUserDeptMpService;
    private final AuthUserMapper authUserMapper;
    private final AuthUserInfraAssembler authUserInfraAssembler;
    private final WinterI18nTemplate winterI18nTemplate;

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
     *
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


}
