package com.winter.cloud.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum LanguageEnum {
    ENGLISH("en_US", "英语"),
    SPANISH("es_ES", "西班牙语"),
    CHINESE("zh_CN", "中文"),
    FRENCH("fr_FR", "法语"),
    RUSSIAN("ru_RU", "俄语"),
    ARABIC("ar_SA", "阿拉伯语");
    ;

    private String code;
    private String message;
}
