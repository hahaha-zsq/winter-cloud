package com.winter.cloud.auth.application.service.impl;


import com.winter.cloud.auth.api.dto.query.PostQuery;
import com.winter.cloud.auth.api.dto.response.PostResponseDTO;
import com.winter.cloud.auth.application.assembler.AuthPostAppAssembler;
import com.winter.cloud.auth.application.service.AuthPostAppService;
import com.winter.cloud.auth.domain.model.entity.AuthPostDO;
import com.winter.cloud.auth.domain.repository.AuthPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthPostAppServiceImpl implements AuthPostAppService {

    private final AuthPostRepository authPostRepository;
    private final AuthPostAppAssembler authPostAppAssembler;


    @Override
    public List<PostResponseDTO> postDynamicQueryList(PostQuery postQuery) {
        log.info("根据职位名和状态查询职位信息，postName={}, status={}", postQuery.getPostName(), postQuery.getStatus());
        List<AuthPostDO> allPostInfo = authPostRepository.postDynamicQueryList(postQuery);
        return authPostAppAssembler.toDTOList(allPostInfo);
    }
}