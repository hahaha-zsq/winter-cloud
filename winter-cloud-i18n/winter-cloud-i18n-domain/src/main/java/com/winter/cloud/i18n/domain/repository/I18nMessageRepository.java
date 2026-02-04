package com.winter.cloud.i18n.domain.repository;

import com.winter.cloud.common.response.PageDTO;
import com.winter.cloud.i18n.api.dto.command.TranslateCommand;
import com.winter.cloud.i18n.api.dto.command.UpsertI18NCommand;
import com.winter.cloud.i18n.api.dto.query.I18nMessageQuery;
import com.winter.cloud.i18n.domain.model.entity.I18nMessageDO;
import com.winter.cloud.i18n.domain.model.entity.TranslateDO;
import com.zsq.i18n.service.I18nMessageService;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 国际化消息仓储接口
 * 继承 Winter I18n 框架的 I18nMessageService 接口
 * 提供国际化消息获取和查询功能
 */
public interface I18nMessageRepository extends I18nMessageService {

    List<I18nMessageDO> getI18nMessageInfo(I18nMessageQuery query);
    String findMessageByKeyAndLocale(String messageKey, String locale);
    String getMessage(String messageKey);
    String getMessage(String messageKey,Object[] args);
    String getMessage(String messageKey, Object[] args, String defaultMessage);

    TranslateDO translate(TranslateCommand translateCommand) throws ExecutionException, InterruptedException;

    PageDTO<I18nMessageDO> i18nPage(I18nMessageQuery i18nMessageQuery);

    Boolean i18nSave(UpsertI18NCommand upsertI18NCommand);
    Boolean hasDuplicateI18nMessage(Long id,String locale, String messageKey,String type);

    Boolean i18nUpdate(UpsertI18NCommand upsertI18NCommand);

    Boolean i18nDelete(List<Long> ids);

    void scheduledRebuildBloomFilter();

    void i18nExportExcel(HttpServletResponse response);

    void i18nImportExcel(HttpServletResponse response, MultipartFile file) throws IOException;

    void i18nExportExcelTemplate(HttpServletResponse response);
}
