package com.winter.cloud.auth.infrastructure.repository;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.winter.cloud.common.exception.BusinessException;
import com.winter.cloud.common.response.PageDTO;
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
        authUserPO.setDelFlag("0");
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
}
