package com.winter.cloud.auth.infrastructure.repository;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.winter.cloud.auth.api.dto.query.PostQuery;
import com.winter.cloud.auth.domain.model.entity.AuthPostDO;
import com.winter.cloud.auth.domain.repository.AuthPostRepository;
import com.winter.cloud.auth.infrastructure.assembler.AuthPostInfraAssembler;
import com.winter.cloud.auth.infrastructure.entity.AuthPostPO;
import com.winter.cloud.auth.infrastructure.service.IAuthPostMPService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AuthPostRepositoryImpl implements AuthPostRepository {

    private final IAuthPostMPService authPostMpService;
    private final AuthPostInfraAssembler authPostInfraAssembler;

    @Override
    public List<AuthPostDO> postDynamicQueryList(PostQuery postQuery) {
        LambdaQueryWrapper<AuthPostPO> queryWrapper = new LambdaQueryWrapper<AuthPostPO>()
                .eq(ObjectUtil.isNotEmpty(postQuery.getId()), AuthPostPO::getId, postQuery.getId())
                .like(ObjectUtil.isNotEmpty(postQuery.getPostCode()), AuthPostPO::getPostCode, postQuery.getPostCode())
                .like(ObjectUtil.isNotEmpty(postQuery.getPostName()), AuthPostPO::getPostName, postQuery.getPostName())
                .eq(ObjectUtil.isNotEmpty(postQuery.getStatus()), AuthPostPO::getStatus, postQuery.getStatus());
        List<AuthPostPO> list = authPostMpService.list(queryWrapper);
        return authPostInfraAssembler.toDOList(list);
    }

    @Override
    public AuthPostDO postDynamicQuery(PostQuery postQuery) {
        LambdaQueryWrapper<AuthPostPO> queryWrapper = new LambdaQueryWrapper<AuthPostPO>()
                .eq(ObjectUtil.isNotEmpty(postQuery.getId()), AuthPostPO::getId, postQuery.getId())
                .like(ObjectUtil.isNotEmpty(postQuery.getPostCode()), AuthPostPO::getPostCode, postQuery.getPostCode())
                .like(ObjectUtil.isNotEmpty(postQuery.getPostName()), AuthPostPO::getPostName, postQuery.getPostName())
                .eq(ObjectUtil.isNotEmpty(postQuery.getStatus()), AuthPostPO::getStatus, postQuery.getStatus());
        AuthPostPO authPostPO = authPostMpService.getOne(queryWrapper);
        return authPostInfraAssembler.toDO(authPostPO);
    }
}
