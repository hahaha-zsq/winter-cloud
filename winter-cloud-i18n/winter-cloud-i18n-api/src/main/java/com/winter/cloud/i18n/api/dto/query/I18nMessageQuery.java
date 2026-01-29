package com.winter.cloud.i18n.api.dto.query;

import com.winter.cloud.common.response.PageAndOrderDTO;
import com.zsq.winter.validation.annotation.DynamicEnum;
import lombok.*;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class I18nMessageQuery extends PageAndOrderDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 消息键
     */
    private String messageKey;

    /**
     * 消息值
     */
    private String messageValue;

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

    /**
     * 类型 1:后端 2:前端
     */
    private String type;

}
