package com.winter.cloud.i18n.api.dto.command;

import lombok.Data;

import java.util.List;

@Data
public class TranslateCommand {

    private String sourceContent;
    //todo 语言来源和目标语言后续需要添加上校验
    private String sourceLanguage;

    private List<String> targetLanguageList;
}
