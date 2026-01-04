package com.winter.cloud.dict.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.winter.cloud.dict.domain.model.entity.DictDataDO;
import com.winter.cloud.dict.infrastructure.entity.DictDataPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 字典数据 Mapper 接口
 * 
 * @author zsq
 */
public interface DictDataMapper extends BaseMapper<DictDataPO> {
    List<DictDataDO> getDictDataByType(@Param("dictType") Long dictType, @Param("status") String status);
}
