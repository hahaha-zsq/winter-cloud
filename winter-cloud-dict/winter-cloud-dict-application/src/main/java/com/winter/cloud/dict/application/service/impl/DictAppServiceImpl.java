package com.winter.cloud.dict.application.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.exception.BusinessException;
import com.winter.cloud.common.response.PageAndOrderDTO;
import com.winter.cloud.common.response.PageDTO;
import com.winter.cloud.dict.api.dto.command.UpsertDictDataCommand;
import com.winter.cloud.dict.api.dto.command.UpsertDictTypeCommand;
import com.winter.cloud.dict.api.dto.query.DictDataQuery;
import com.winter.cloud.dict.api.dto.query.DictTypeQuery;
import com.winter.cloud.dict.api.dto.response.DictDataDTO;
import com.winter.cloud.dict.api.dto.response.DictTypeDTO;
import com.winter.cloud.dict.application.assembler.DictDataAppAssembler;
import com.winter.cloud.dict.application.assembler.DictTypeAppAssembler;
import com.winter.cloud.dict.application.service.DictAppService;
import com.winter.cloud.dict.domain.model.entity.DictDataDO;
import com.winter.cloud.dict.domain.model.entity.DictTypeDO;
import com.winter.cloud.dict.domain.repository.DictDataRepository;
import com.winter.cloud.dict.domain.repository.DictTypeRepository;
import com.winter.cloud.dict.infrastructure.entity.DictTypePO;
import com.zsq.i18n.template.WinterI18nTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DictAppServiceImpl implements DictAppService {
    private final DictDataRepository dictDataRepository;
    private final DictTypeRepository dictTypeRepository;
    private final DictDataAppAssembler dictDataAppAssembler;
    private final DictTypeAppAssembler dictTypeAppAssembler;
    private final WinterI18nTemplate winterI18nTemplate;
    @Override
    public List<DictDataDTO> getDictDataByType(Long dictType, String status) {
        List<DictDataDO> data = dictDataRepository.getDictDataByType(dictType, status);
        return dictDataAppAssembler.toDictDataDTOList(data);
    }

    @Override
    public List<DictDataDTO> dictValueDynamicQueryList(DictDataQuery dictQuery) {
        List<DictDataDO> data = dictDataRepository.dictValueDynamicQueryList(dictQuery);
        return dictDataAppAssembler.toDictDataDTOList(data);
    }

    @Override
    public PageDTO<DictTypeDTO> dictTypePage(DictTypeQuery dictTypeQuery) {
        PageDTO<DictTypeDO> doPage = dictTypeRepository.dictTypePage(dictTypeQuery);
        List<DictTypeDTO> dtoList = dictTypeAppAssembler.toDictTypeDTOList(doPage.getRecords());
        return new PageDTO<>(dtoList, doPage.getTotal());
    }

    @Override
    public Boolean dictTypeSave(UpsertDictTypeCommand upsertDictTypeCommand) {
        return dictTypeRepository.dictTypeSave(upsertDictTypeCommand);
    }

    @Override
    public Boolean dictTypeUpdate(UpsertDictTypeCommand upsertDictTypeCommand) {
        return dictTypeRepository.dictTypeUpdate(upsertDictTypeCommand);
    }

    @Override
    public Boolean dictTypeDelete(List<Long> ids) {
        return dictTypeRepository.dictTypeDelete(ids);
    }

    @Override
    public PageDTO<DictDataDTO> dictDataPage(DictDataQuery dictQuery) {
        if (ObjectUtils.isEmpty(dictQuery) || ObjectUtils.isEmpty(dictQuery.getOrders())) {
            dictQuery.setOrders(Collections.emptyList());
        }
        List<PageAndOrderDTO.OrderDTO> normalizedOrders = normalizeAndValidateOrders(dictQuery.getOrders());

        dictQuery.setOrders(normalizedOrders);

        PageDTO<DictDataDO> doPage = dictDataRepository.dictDataPage(dictQuery);
        List<DictDataDTO> dtoList = dictDataAppAssembler.toDictDataDTOList(doPage.getRecords());
        return new PageDTO<>(dtoList, doPage.getTotal());
    }

    @Override
    public Boolean dictDataSave(List<UpsertDictDataCommand> upsertDictDataCommandList) {
        List<DictDataDO> doList = dictDataAppAssembler.toDOList(upsertDictDataCommandList);
        return dictDataRepository.dictDataSave(doList);
    }

    @Override
    public Boolean dictDataUpdate(UpsertDictDataCommand upsertDictDataCommand) {
        DictDataDO aDo = dictDataAppAssembler.toDO(upsertDictDataCommand);
        return dictDataRepository.dictDataUpdate(aDo);
    }

    @Override
    public Boolean dictDataDelete(List<Long> ids) {
        return dictDataRepository.dictDataDelete(ids);
    }

    @Override
    public List<DictTypeDTO> dictTypeList(DictTypeQuery dictTypeQuery) {
        List<DictTypeDO> dictTypeDOList = dictTypeRepository.dictTypeList(dictTypeQuery);
        return dictTypeAppAssembler.toDictTypeDTOList(dictTypeDOList);
    }


    /**
     * 对排序参数进行【校验 + 标准化】的统一处理方法
     * <p>
     * 主要职责：
     * 1. 校验排序字段 field 是否在允许的白名单中（防 SQL 注入）
     * 2. 校验排序方式 order 是否合法（asc / desc / ascend / descend）
     * 3. 按 sequence 字段对排序条件进行排序（保证多字段排序顺序正确）
     * 4. 将排序方式统一标准化为数据库可识别的 "asc" / "desc"
     *
     * @param orders 前端传入的排序参数列表
     * @return 经过校验、排序、标准化后的排序参数列表
     */
    private List<PageAndOrderDTO.OrderDTO> normalizeAndValidateOrders(
            List<PageAndOrderDTO.OrderDTO> orders) {

        // 1️⃣ 如果前端未传排序参数，直接返回空列表
        // 由上层逻辑决定是否使用默认排序
        if (ObjectUtils.isEmpty(orders)) {
            return Collections.emptyList();
        }

        // 2️⃣ 允许排序的字段白名单（数据库真实字段名）
        // ⚠️ 这里是防 SQL 注入的关键点
        Set<String> allowedFields = Set.of(
                "dict_sort",
                "status",
                "create_time",
                "dict_type_id"
        );

        // 3️⃣ 允许的排序方式（前端可能传多种写法）
        // 后续会统一转换为 asc / desc
        Set<String> allowedOrders = Set.of(
                "ascend",
                "asc",
                "descend",
                "desc"
        );

        return orders.stream()
                // 4️⃣ 校验阶段（不修改数据，只做合法性检查）
                .peek(order -> {
                    String field = order.getField();
                    String orderStr = order.getOrder();
                    // 4.1 校验排序字段是否合法
                    //  - 不能为空
                    //  - 必须在白名单中
                    if (ObjectUtils.isEmpty(field) || !allowedFields.contains(field)) {
                        throw new BusinessException(
                                ResultCodeEnum.FAIL_LANG.getCode(),
                                winterI18nTemplate.message(
                                        CommonConstants.I18nKey.SORT_KEY_ILLEGAL
                                )
                        );
                    }
                    // 4.2 校验排序方式是否合法
                    //  - 不能为空
                    //  - 忽略大小写后必须在允许集合中
                    if (ObjectUtils.isEmpty(orderStr)
                        || !allowedOrders.contains(orderStr.toLowerCase())) {
                        throw new BusinessException(
                                ResultCodeEnum.FAIL_LANG.getCode(),
                                winterI18nTemplate.message(
                                        CommonConstants.I18nKey.SORT_ORDER_ILLEGAL
                                )
                        );
                    }
                })

                // 5️⃣ 按 sequence 字段排序
                // 确保多字段排序时，顺序与前端约定一致
                .sorted(Comparator.comparing(PageAndOrderDTO.OrderDTO::getSequence))

                // 6️⃣ 对排序方式进行标准化处理（ascend → asc，descend → desc）
                .map(this::normalizeOrderValue)

                // 7️⃣ 收集为 List
                .collect(Collectors.toList());
    }

    /**
     * 对单个排序参数的 order 值进行标准化处理
     * <p>
     * 目标：
     * - 将前端可能传入的多种排序写法：
     * ascend / ASCEND / asc → asc
     * descend / DESCEND / desc → desc
     * - 返回一个新的 OrderDTO，避免直接修改原对象（更安全）
     *
     * @param dto 原始排序参数对象
     * @return 标准化后的排序参数对象
     */
    private PageAndOrderDTO.OrderDTO normalizeOrderValue(PageAndOrderDTO.OrderDTO dto) {

        String order = dto.getOrder();
        // （如果允许 order 为空，通常应在上游已拦截）
        if (!ObjectUtils.isEmpty(order)) {
            String lower = order.toLowerCase();
            // 将前端排序方式统一为数据库可用的写法
            if ("ascend".equals(lower) || "asc".equals(lower)) {
                order = "asc";
            } else if ("descend".equals(lower) || "desc".equals(lower)) {
                order = "desc";
            }
        }
        // 创建新的 OrderDTO 对象，避免修改原始参数对象
        // 如果 OrderDTO 是不可变对象（builder / withXxx），会更理想
        PageAndOrderDTO.OrderDTO normalized = new PageAndOrderDTO.OrderDTO();
        normalized.setField(dto.getField());
        normalized.setOrder(order);
        normalized.setSequence(dto.getSequence());

        return normalized;
    }
}
