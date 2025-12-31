package com.winter.cloud.auth.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.winter.cloud.auth.infrastructure.entity.AuthUserPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户表 Mapper 接口
 * 继承 BaseMapper 后自动拥有 CRUD 能力
 */
public interface AuthUserMapper extends BaseMapper<AuthUserPO> {
    List<String> getRoleKeyList(@Param("userId") Long userId);
    // 如果有复杂的自定义 SQL，可以在这里定义方法并在 XML 中实现
}