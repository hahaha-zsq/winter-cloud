package com.winter.cloud.auth.infrastructure.repository;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
    public List<AuthPostDO> getAllPostInfo(String postName, String status) {
        LambdaQueryWrapper<AuthPostPO> queryWrapper = new LambdaQueryWrapper<AuthPostPO>()
                .like(AuthPostPO::getPostName, postName)
                .eq(ObjectUtil.isNotEmpty(status), AuthPostPO::getStatus, status);
        List<AuthPostPO> list = authPostMpService.list(queryWrapper);
        return authPostInfraAssembler.toDOList(list);
    }
}
