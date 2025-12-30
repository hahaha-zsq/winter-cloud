package com.winter.cloud.auth.infrastructure.repository;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.winter.cloud.auth.api.dto.command.UserRegisterCommand;
import com.winter.cloud.auth.domain.model.entity.AuthUserDO;
import com.winter.cloud.auth.domain.repository.AuthUserRepository;
import com.winter.cloud.auth.infrastructure.assembler.AuthUserInfraAssembler;
import com.winter.cloud.auth.infrastructure.entity.AuthUserPO;
import com.winter.cloud.auth.infrastructure.service.IAuthUserMpService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AuthUserRepositoryImpl implements AuthUserRepository {
    private final IAuthUserMpService authUserMpService;
    private final AuthUserInfraAssembler authUserInfraAssembler;

    @Override
    public AuthUserDO findById(Long id) {
        return null;
    }

    @Override
    public AuthUserDO findByUserName(String userName) {
        AuthUserPO po = authUserMpService.getOne(new LambdaQueryWrapper<AuthUserPO>().eq(AuthUserPO::getUserName, userName));
        return authUserInfraAssembler.toDO(po);
    }

    @Override
    public Boolean save(AuthUserDO authUserDo) {
        AuthUserPO po = authUserInfraAssembler.toPO(authUserDo);
        return authUserMpService.save(po);
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
}
