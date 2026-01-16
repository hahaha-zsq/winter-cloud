package com.winter.cloud.auth.domain.repository;

import com.winter.cloud.auth.api.dto.response.IconResponseDTO;
import com.winter.cloud.auth.domain.model.entity.IconTypeDO;
import com.winter.cloud.auth.domain.model.entity.IconValueDO;

import java.util.List;


public interface IconRepository {
    Long saveIconType(IconTypeDO iconTypeDO);
    void saveIconValue(List<IconValueDO> iconValueDOList);

    List<IconResponseDTO> getIconList(String name);
}
