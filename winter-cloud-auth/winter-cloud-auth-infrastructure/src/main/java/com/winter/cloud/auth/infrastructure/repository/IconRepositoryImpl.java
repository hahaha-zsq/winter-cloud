package com.winter.cloud.auth.infrastructure.repository;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.winter.cloud.auth.api.dto.response.IconResponseDTO;
import com.winter.cloud.auth.domain.model.entity.IconTypeDO;
import com.winter.cloud.auth.domain.model.entity.IconValueDO;
import com.winter.cloud.auth.domain.repository.IconRepository;
import com.winter.cloud.auth.infrastructure.assembler.IconInfraAssembler;
import com.winter.cloud.auth.infrastructure.entity.IconTypePO;
import com.winter.cloud.auth.infrastructure.entity.IconValuePO;
import com.winter.cloud.auth.infrastructure.mapper.IconValueMapper;
import com.winter.cloud.auth.infrastructure.service.IconTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class IconRepositoryImpl implements IconRepository {
    private final IconTypeService iconTypeService;
    private final IconValueMapper iconValueMapper;

    private final IconInfraAssembler iconInfraAssembler;

    @Override
    public Long saveIconType(IconTypeDO iconTypeDO) {
        IconTypePO po = iconInfraAssembler.toPO(iconTypeDO);
        // 判断图标类型是否已存在
        IconTypePO one = iconTypeService.getOne(new LambdaQueryWrapper<IconTypePO>().eq(IconTypePO::getPrefix, iconTypeDO.getPrefix()));
        if (ObjectUtil.isEmpty(one)) {
            // 不存在直接新增
            iconTypeService.save(po);
            return po.getId();
        } else {
            // 存在获取该类型的id
            return one.getId();
        }
    }

    @Override
    public void saveIconValue(List<IconValueDO> iconValueDOList) {
        List<IconValuePO> poList = iconInfraAssembler.toPOList(iconValueDOList);
        iconValueMapper.batchInsertOrUpdateIconValue(poList);

    }

    @Override
    public List<IconResponseDTO> getIconList(String name) {
        return iconValueMapper.getIconList(name);
    }
}
