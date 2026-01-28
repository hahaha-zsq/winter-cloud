package com.winter.cloud.i18n.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.winter.cloud.i18n.api.dto.query.I18nMessageQuery;
import com.winter.cloud.i18n.infrastructure.entity.I18nMessagePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 国际化消息 Mapper
 */
@Mapper
public interface I18nMessageMapper extends BaseMapper<I18nMessagePO> {

    List<I18nMessagePO> getI18nMessageInfo(@Param("query") I18nMessageQuery query);

    IPage<I18nMessagePO> selectI18nPage(Page <I18nMessagePO> page, @Param("query") I18nMessageQuery query);
}
