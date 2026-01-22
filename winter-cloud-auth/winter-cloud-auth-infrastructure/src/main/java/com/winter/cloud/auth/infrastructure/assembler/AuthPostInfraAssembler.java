package com.winter.cloud.auth.infrastructure.assembler;

import com.winter.cloud.auth.api.dto.response.PostResponseDTO;
import com.winter.cloud.auth.domain.model.entity.AuthPostDO;
import com.winter.cloud.auth.infrastructure.entity.AuthPostPO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * 基础设施层转换器
 * 负责 PO <-> Entity 互转
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthPostInfraAssembler {

    // 也可以定义 List 的转换
    List<AuthPostDO> toDOList(List<AuthPostPO> authPostPOList);
    AuthPostDO toDO(AuthPostPO authPostPO);

}