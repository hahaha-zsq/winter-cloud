package com.winter.cloud.i18n.infrastructure.repository;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.winter.cloud.i18n.api.dto.query.I18nMessageQuery;
import com.winter.cloud.i18n.domain.model.entity.I18nMessageDO;
import com.winter.cloud.i18n.domain.repository.I18nMessageRepository;
import com.winter.cloud.i18n.infrastructure.assembler.I18nMessageInfraAssembler;
import com.winter.cloud.i18n.infrastructure.entity.I18nMessagePO;
import com.winter.cloud.i18n.infrastructure.mapper.I18nMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
@Slf4j
@RequiredArgsConstructor
public class I18nMessageRepositoryImpl implements I18nMessageRepository {
    private final I18nMessageMapper messageMapper;
    private final I18nMessageInfraAssembler i18nMessageInfraAssembler;

    /**
     * 根据消息键和语言环境查询消息内容
     * <p>直接查询数据库,不经过缓存。</p>
     *
     * @param messageKey 消息键
     * @param locale     语言环境
     * @return 消息值
     * @apiNote 注意：此方法不走缓存，仅用于缓存未命中时的回源查询，请勿在高频业务中直接调用。
     */
    @Override
    public String findMessageByKeyAndLocale(String messageKey, String locale) {
        LambdaQueryWrapper<I18nMessagePO> queryWrapper = new LambdaQueryWrapper<I18nMessagePO>()
                .eq(I18nMessagePO::getMessageKey, messageKey)
                .eq(I18nMessagePO::getLocale, locale);
        I18nMessagePO i18nMessagePO = messageMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNotEmpty(i18nMessagePO)) {
            return i18nMessagePO.getMessageValue();
        }
        return null;
    }

    @Override
    public List<I18nMessageDO> getI18nMessageInfo(I18nMessageQuery query) {
        List<I18nMessagePO> i18nMessageInfo = messageMapper.getI18nMessageInfo(query);
        return i18nMessageInfraAssembler.toDOList(i18nMessageInfo);
    }
}