package com.winter.cloud.dict.infrastructure.assembler;

import com.winter.cloud.dict.domain.model.entity.DictDataDO;
import com.winter.cloud.dict.infrastructure.entity.DictDataPO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * 基础设施层转换器
 * 负责 PO <-> Entity 互转
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DictDataInfraAssembler {

    // PO 转 Entity
    DictDataDO toDO(DictDataPO dictDataPO);

    // Entity 转 PO
    DictDataPO toPO(DictDataDO dictDataDO);
    List<DictDataPO> toPOList(List<DictDataDO> dictDataDOList);

    // 也可以定义 List 的转换
    List<DictDataDO> toDOList(List<DictDataPO> dictDataPOList);
}