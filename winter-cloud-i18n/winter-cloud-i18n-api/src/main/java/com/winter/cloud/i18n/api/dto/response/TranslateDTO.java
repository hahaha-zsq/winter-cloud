package com.winter.cloud.i18n.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TranslateDTO {
    private String sourceContent;
    private String sourceLanguage;
    // 翻译后的目标语言，key为targetLanguage
    private Map<String,TargetLanguageDTO> targetLanguageDTO;

    @Data
    public static class TargetLanguageDTO {
        private String targetLanguage;
        private String targetContent;
    }
}


