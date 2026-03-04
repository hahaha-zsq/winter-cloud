package com.winter.cloud.auth.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.winter.cloud.auth.api.dto.query.PostQuery;
import com.winter.cloud.auth.infrastructure.entity.AuthPostPO;
import com.winter.cloud.auth.infrastructure.entity.AuthUserPO;
import org.apache.ibatis.annotations.Param;


public interface AuthPostMapper extends BaseMapper<AuthPostPO> {

    IPage<AuthPostPO> selectPostPage(Page<AuthUserPO> page, @Param("query") PostQuery query);
}
