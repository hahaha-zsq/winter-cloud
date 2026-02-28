package com.winter.cloud.auth.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.winter.cloud.auth.infrastructure.entity.AuthUserDeptPO;

/**
 * 继承 BaseMapper 后自动拥有 CRUD 能力
 */
public interface AuthUserDeptMapper extends BaseMapper<AuthUserDeptPO> {
    // 如果有复杂的自定义 SQL，可以在这里定义方法并在 XML 中实现
}