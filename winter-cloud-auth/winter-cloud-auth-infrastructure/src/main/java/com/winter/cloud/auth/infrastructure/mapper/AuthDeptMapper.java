package com.winter.cloud.auth.infrastructure.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.winter.cloud.auth.infrastructure.entity.AuthDeptPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AuthDeptMapper extends BaseMapper<AuthDeptPO> {
    List<AuthDeptPO> selectDeptListByUserId(@Param("userId") Long userId, @Param("status") String status);
}
