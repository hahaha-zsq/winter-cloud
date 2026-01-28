package com.winter.cloud.i18n.application.service.impl;

import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.exception.BusinessException;
import com.winter.cloud.common.response.PageAndOrderDTO;
import com.winter.cloud.common.response.PageDTO;
import com.winter.cloud.i18n.api.dto.command.TranslateCommand;
import com.winter.cloud.i18n.api.dto.query.I18nMessageQuery;
import com.winter.cloud.i18n.api.dto.response.I18nMessageDTO;
import com.winter.cloud.i18n.api.dto.response.TranslateDTO;
import com.winter.cloud.i18n.application.assembler.I18nMessageAppAssembler;
import com.winter.cloud.i18n.application.service.I18nMessageAppService;
import com.winter.cloud.i18n.domain.model.entity.I18nMessageDO;
import com.winter.cloud.i18n.domain.model.entity.TranslateDO;
import com.winter.cloud.i18n.domain.repository.I18nMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class I18nMessageAppServiceImpl implements I18nMessageAppService {
    private final I18nMessageRepository i18nMessageRepository;
    private final I18nMessageAppAssembler i18nMessageAppAssembler;

    @Override
    public List<I18nMessageDTO> getI18nMessageInfo(I18nMessageQuery query) {
        List<I18nMessageDO> data = i18nMessageRepository.getI18nMessageInfo(query);
        return i18nMessageAppAssembler.toI18nMessageDTOList(data);
    }

    @Override
    public TranslateDTO translate(TranslateCommand translateCommand) {
        TranslateDO translateDO = i18nMessageRepository.translate(translateCommand);
        return i18nMessageAppAssembler.toTranslateDTO(translateDO);
    }

    @Override
    public PageDTO<I18nMessageDTO> i18nPage(I18nMessageQuery i18nMessageQuery) {
        List<PageAndOrderDTO.OrderDTO> orderDTOList = i18nMessageQuery.getOrders();
        List<String> allowSortColumnList = List.of("locale", "create_time");
        List<String> allowSortValue = List.of("ascend", "asc", "descend", "desc", "ASCEND", "ASC", "DESCEND", "DESC");
        // 判断排序字段是否在允许的字段列表中，只要有一个不在，就抛出异常
        orderDTOList.forEach(orderDTO -> {
            if (!allowSortColumnList.contains(orderDTO.getField())) {
                throw new BusinessException(ResultCodeEnum.FAIL_LANG.getCode(), "非法的排序字段！");
            }
            if (!allowSortValue.contains(orderDTO.getOrder())) {
                throw new BusinessException(ResultCodeEnum.FAIL_LANG.getCode(), "非法的排序方式！");
            }
        });
        // 对排序字段进行排序
        List<PageAndOrderDTO.OrderDTO> collect = orderDTOList.stream().sorted((o1, o2) -> o1.getSequence().compareTo(o2.getSequence()))
                .map(dto -> {
                    String newOrder = dto.getOrder();
                    if (newOrder != null) {
                        String lower = newOrder.toLowerCase();
                        if ("ascend".equals(lower)) {
                            newOrder = "asc";
                        } else if ("descend".equals(lower)) {
                            newOrder = "desc";
                        }
                    }
                    PageAndOrderDTO.OrderDTO orderDTO = new PageAndOrderDTO.OrderDTO();
                    orderDTO.setField(dto.getField());
                    orderDTO.setOrder(newOrder);
                    orderDTO.setSequence(dto.getSequence());
                    return orderDTO;
                }).collect(Collectors.toList());

        i18nMessageQuery.setOrders(collect);

        PageDTO<I18nMessageDO> doPage = i18nMessageRepository.i18nPage(i18nMessageQuery);
        List<I18nMessageDTO> userResponseDTOList = i18nMessageAppAssembler.toI18nMessageDTOList(doPage.getRecords());
        return new PageDTO<>(userResponseDTOList, doPage.getTotal());
    }

    @Override
    public String findMessageByKeyAndLocale(String messageKey, String locale) {
        return i18nMessageRepository.findMessageByKeyAndLocale(messageKey, locale);
    }

    @Override
    public String getMessage(String messageKey) {
        return i18nMessageRepository.getMessage(messageKey);
    }

    @Override
    public String getMessage(String messageKey, Locale locale) {
        return i18nMessageRepository.getMessage(messageKey, locale);
    }

    @Override
    public String getMessage(String messageKey, Object[] args) {
        return i18nMessageRepository.getMessage(messageKey, args);
    }

    @Override
    public String getMessage(String messageKey, Object[] args, Locale locale) {
        return i18nMessageRepository.getMessage(messageKey, args, locale);

    }

    @Override
    public String getMessage(String messageKey, Object[] args, String defaultMessage, Locale locale) {
        return i18nMessageRepository.getMessage(messageKey, args, defaultMessage, locale);

    }

    @Override
    public String getMessage(String messageKey, Object[] args, String defaultMessage) {
        return i18nMessageRepository.getMessage(messageKey, args, defaultMessage);
    }
}