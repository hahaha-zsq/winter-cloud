package com.winter.cloud.auth.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.winter.cloud.auth.api.dto.response.IconResponseDTO;
import com.winter.cloud.auth.infrastructure.entity.IconValuePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IconValueMapper extends BaseMapper<IconValuePO> {
    void batchInsertOrUpdateIconValue(@Param("list") List<IconValuePO> list);

    List<IconResponseDTO> getIconList(@Param("name") String name);
}