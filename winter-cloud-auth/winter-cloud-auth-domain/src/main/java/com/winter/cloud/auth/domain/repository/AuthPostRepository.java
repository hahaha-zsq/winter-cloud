package com.winter.cloud.auth.domain.repository;


import com.winter.cloud.auth.domain.model.entity.AuthPostDO;

import java.util.List;

/**
 * 职位仓储接口 (面向领域)
 */
public interface AuthPostRepository {

    List<AuthPostDO> getAllPostInfo(String postName, String status);
}