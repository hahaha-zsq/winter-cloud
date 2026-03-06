package com.winter.cloud.dict.infrastructure.repository;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.common.exception.BusinessException;
import com.winter.cloud.common.response.PageDTO;
import com.winter.cloud.dict.api.dto.command.UpsertDictTypeCommand;
import com.winter.cloud.dict.api.dto.query.DictTypeQuery;
import com.winter.cloud.dict.api.dto.response.DictTypeDTO;
import com.winter.cloud.dict.domain.model.entity.DictTypeDO;
import com.winter.cloud.dict.domain.repository.DictTypeRepository;
import com.winter.cloud.dict.infrastructure.assembler.DictTypeInfraAssembler;
import com.winter.cloud.dict.infrastructure.entity.DictDataPO;
import com.winter.cloud.dict.infrastructure.entity.DictTypePO;
import com.winter.cloud.dict.infrastructure.service.IDictDataMPService;
import com.winter.cloud.dict.infrastructure.service.IDictTypeMPService;
import com.zsq.i18n.template.WinterI18nTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.winter.cloud.common.enums.ResultCodeEnum.DUPLICATE_KEY;

@Repository
@RequiredArgsConstructor
public class DictTypeRepositoryImpl implements DictTypeRepository {
    private final IDictTypeMPService dictTypeMPService;
    private final IDictDataMPService dictDataMPService;
    private final DictTypeInfraAssembler dictTypeInfraAssembler;
    private final WinterI18nTemplate winterI18nTemplate;

    @Override
    public PageDTO<DictTypeDO> dictTypePage(DictTypeQuery dictTypeQuery) {
        Page<DictTypePO> page = new Page<>(dictTypeQuery.getPageNum(), dictTypeQuery.getPageSize());
        LambdaQueryWrapper<DictTypePO> queryWrapper = new LambdaQueryWrapper<DictTypePO>()
                .eq(ObjectUtil.isNotEmpty(dictTypeQuery.getId()), DictTypePO::getId, dictTypeQuery.getId())
                .like(ObjectUtil.isNotEmpty(dictTypeQuery.getDictName()), DictTypePO::getDictName, dictTypeQuery.getDictName());
        IPage<DictTypePO> pageResult = dictTypeMPService.page(page, queryWrapper);
        List<DictTypeDO> doList = dictTypeInfraAssembler.toDOList(pageResult.getRecords());
        return new PageDTO<>(doList, pageResult.getTotal());
    }

    @Override
    public Boolean hasDuplicateDictType(DictTypeDO aDo) {
        LambdaQueryWrapper<DictTypePO> dictTypePOLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dictTypePOLambdaQueryWrapper.eq(DictTypePO::getDictName, aDo.getDictName()).ne(ObjectUtil.isNotEmpty(aDo.getId()), DictTypePO::getId, aDo.getId());
        long count = dictTypeMPService.count(dictTypePOLambdaQueryWrapper);
        return count > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean dictTypeSave(UpsertDictTypeCommand upsertDictTypeCommand) {
        Boolean b = hasDuplicateDictType(DictTypeDO.builder().dictName(upsertDictTypeCommand.getDictName()).id(upsertDictTypeCommand.getId()).build());
        if (b) {
            throw new BusinessException(DUPLICATE_KEY.getCode(), winterI18nTemplate.message(CommonConstants.I18nKey.DICT_TYPE_EXISTS));
        }
        DictTypePO dictTypePO = DictTypePO.builder()
                .dictName(upsertDictTypeCommand.getDictName())
                .remark(upsertDictTypeCommand.getRemark())
                .build();
        return dictTypeMPService.save(dictTypePO);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean dictTypeUpdate(UpsertDictTypeCommand upsertDictTypeCommand) {
        Boolean b = hasDuplicateDictType(DictTypeDO.builder().dictName(upsertDictTypeCommand.getDictName()).id(upsertDictTypeCommand.getId()).build());
        if (b) {
            throw new BusinessException(DUPLICATE_KEY.getCode(), winterI18nTemplate.message(CommonConstants.I18nKey.DICT_TYPE_EXISTS));
        }
        DictTypePO dictTypePO = DictTypePO.builder()
                .id(upsertDictTypeCommand.getId())
                .dictName(upsertDictTypeCommand.getDictName())
                .remark(upsertDictTypeCommand.getRemark())
                .build();
        return dictTypeMPService.updateById(dictTypePO);
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean dictTypeDelete(List<Long> ids) {
         dictTypeMPService.removeByIds(ids);
         dictDataMPService.remove(new LambdaQueryWrapper<DictDataPO>().in(DictDataPO::getDictTypeId, ids));
         return true;
    }

    @Override
    public List<DictTypeDO> dictTypeList(DictTypeQuery dictTypeQuery) {
        LambdaQueryWrapper<DictTypePO> lambdaQueryWrapper = new LambdaQueryWrapper<DictTypePO>().like(ObjectUtil.isNotEmpty(dictTypeQuery.getDictName()), DictTypePO::getDictName, dictTypeQuery.getDictName());
        List<DictTypePO> list = dictTypeMPService.list(lambdaQueryWrapper);
        return dictTypeInfraAssembler.toDOList(list);
    }
}
