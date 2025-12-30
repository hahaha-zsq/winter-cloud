package com.winter.cloud.auth.domain.repository;

import com.winter.cloud.auth.api.dto.command.UserRegisterCommand;
import com.winter.cloud.auth.domain.model.entity.AuthUserDO;

/**
 * 用户仓储接口 (面向领域)
 */
public interface AuthUserRepository {

    /**
     * 根据ID查询用户
     */
    AuthUserDO findById(Long id);

    /**
     * 根据用户名查询用户
     */
    AuthUserDO findByUserName(String userName);

    /**
     * 保存或更新用户
     */
    Boolean save(AuthUserDO authUser);

    /**
     * 删除用户
     */
    void deleteById(Long id);

    // 检查用户名是否存在
    boolean hasDuplicateUser(UserRegisterCommand command);
}