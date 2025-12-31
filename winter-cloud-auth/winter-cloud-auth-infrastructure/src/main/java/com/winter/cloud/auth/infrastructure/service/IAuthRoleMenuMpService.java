package com.winter.cloud.auth.infrastructure.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.winter.cloud.auth.infrastructure.entity.AuthRoleMenuPO;

/**
 * 基础设施层内部使用的 MP Service
 * 继承 IService 以获得批量插入、链式查询等 MP 高级功能
 */
public interface IAuthRoleMenuMpService extends IService<AuthRoleMenuPO> {
}