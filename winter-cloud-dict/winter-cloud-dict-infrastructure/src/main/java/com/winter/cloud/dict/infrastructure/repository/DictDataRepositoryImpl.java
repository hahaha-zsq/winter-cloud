package com.winter.cloud.dict.infrastructure.repository;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.common.exception.BusinessException;
import com.winter.cloud.common.response.PageDTO;
import com.winter.cloud.dict.api.dto.query.DictDataQuery;
import com.winter.cloud.dict.domain.model.entity.DictDataDO;
import com.winter.cloud.dict.domain.repository.DictDataRepository;
import com.winter.cloud.dict.infrastructure.assembler.DictDataInfraAssembler;
import com.winter.cloud.dict.infrastructure.entity.DictDataPO;
import com.winter.cloud.dict.infrastructure.mapper.DictDataMapper;
import com.winter.cloud.dict.infrastructure.service.IDictDataMPService;
import com.zsq.winter.office.service.excel.WinterExcelTemplate;
import com.zsq.winter.redis.ddc.service.WinterRedisTemplate;
import com.zsq.i18n.template.WinterI18nTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Validator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.winter.cloud.common.enums.ResultCodeEnum.DUPLICATE_KEY;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DictDataRepositoryImpl implements DictDataRepository {
    private final DictDataMapper dictDataMapper;
    private final DictDataInfraAssembler dictInfraAssembler;
    private final IDictDataMPService dictDataMPService;
    private final DictDataInfraAssembler dictDataInfraAssembler;
    private final WinterRedisTemplate winterRedisTemplate;
    private final WinterI18nTemplate winterI18nTemplate;
    private final WinterExcelTemplate winterExcelTemplate;
    private final Validator fastFalseValidator;
    private final ObjectMapper objectMapper;

    @Override
    public List<DictDataDO> getDictDataByType(Long dictType, String status) {
        List<DictDataDO> list = dictDataMapper.getDictDataByType(dictType, status);
        List<DictDataPO> poList = dictDataInfraAssembler.toPOList(list);
        Map<Long, List<DictDataPO>> collect = poList.stream().collect(Collectors.groupingBy(DictDataPO::getDictTypeId));
        collect.forEach((k, v) -> {
            String data = null;
            try {
                data = new ObjectMapper().writeValueAsString(v);
            } catch (Exception e) {
                log.error("dict data cache error", e);
            }
            boolean b = winterRedisTemplate.hasKey(CommonConstants.Redis.DICT_KEY + CommonConstants.Redis.SPLIT + k);
            if (!b) {
                winterRedisTemplate.set(CommonConstants.Redis.DICT_KEY + CommonConstants.Redis.SPLIT + k, data);
            }
        });
        return list;
    }

    @Override
    public List<DictDataDO> dictValueDynamicQueryList(DictDataQuery dictQuery) {
        LambdaQueryWrapper<DictDataPO> queryWrapper = new LambdaQueryWrapper<DictDataPO>()
                .eq(ObjectUtil.isNotEmpty(dictQuery.getDictTypeId()), DictDataPO::getDictTypeId, dictQuery.getDictTypeId())
                .eq(ObjectUtil.isNotEmpty(dictQuery.getStatus()), DictDataPO::getStatus, dictQuery.getStatus())
                .like(ObjectUtil.isNotEmpty(dictQuery.getDictLabel()), DictDataPO::getDictLabel, dictQuery.getDictLabel())
                .like(ObjectUtil.isNotEmpty(dictQuery.getDictValue()), DictDataPO::getDictValue, dictQuery.getDictValue());
        List<DictDataPO> list = dictDataMPService.list(queryWrapper);
        return dictDataInfraAssembler.toDOList(list);
    }

    @Override
    public Boolean hasDuplicateDictData(DictDataDO aDo) {
        LambdaQueryWrapper<DictDataPO> dictDataPOLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dictDataPOLambdaQueryWrapper
                .eq(DictDataPO::getDictTypeId, aDo.getDictTypeId())
                .nested(e -> e
                        .eq(DictDataPO::getDictLabel, aDo.getDictLabel())
                        .or()
                        .eq(DictDataPO::getDictValue, aDo.getDictValue()))
                .ne(ObjectUtil.isNotEmpty(aDo.getId()), DictDataPO::getId, aDo.getId());
        long count = dictDataMPService.count(dictDataPOLambdaQueryWrapper);
        return count > 0;
    }

    @Override
    public PageDTO<DictDataDO> dictDataPage(DictDataQuery dictQuery) {
        Page<DictDataPO> page = new Page<>(dictQuery.getPageNum(), dictQuery.getPageSize());
        IPage<DictDataPO> pageResult = dictDataMapper.selectDictDataPage(page, dictQuery);
        List<DictDataDO> doList = dictDataInfraAssembler.toDOList(pageResult.getRecords());
        return new PageDTO<>(doList, pageResult.getTotal());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean dictDataSave(List<DictDataDO> dictDataDOList) {
        for (DictDataDO dictDataDO : dictDataDOList) {
            if (hasDuplicateDictData(dictDataDO)) {
                throw new BusinessException(DUPLICATE_KEY.getCode(), winterI18nTemplate.message(CommonConstants.I18nKey.DICT_DATA_DUPLICATED));
            }
        }
        List<DictDataPO> poList = dictDataInfraAssembler.toPOList(dictDataDOList);
        return dictDataMPService.saveBatch(poList);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean dictDataUpdate(DictDataDO dictDataDO) {
        if (hasDuplicateDictData(dictDataDO)) {
            throw new BusinessException(DUPLICATE_KEY.getCode(), winterI18nTemplate.message(CommonConstants.I18nKey.DICT_DATA_DUPLICATED));
        }
        DictDataPO po = dictInfraAssembler.toPO(dictDataDO);
        return dictDataMPService.updateById(po);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean dictDataDelete(List<Long> ids) {
        return dictDataMPService.removeByIds(ids);
    }

}
