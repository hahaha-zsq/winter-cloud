package com.winter.cloud.i18n.api.dto.query;

import com.zsq.winter.validation.annotation.DynamicEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class I18nMessageQuery {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 消息键
     */
    private String messageKey;

    /**
     * 语言环境
     * 1. dictType 是必须参数，随便填一个标识即可（如 "gender"）
     * 2. fixedValues 定义允许的值列表
     * 3. allowNull = false 表示必填（默认就是 false）
     */
    @DynamicEnum(
            dictType = "115",
            message = "语言环境格式不符合要求"
    )
    private String locale;

}
