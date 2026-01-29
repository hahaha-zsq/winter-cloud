package com.winter.cloud.i18n.api.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 国际化消息响应对象
 */
@Data
public class I18nMessageDTO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 消息键
     */
    private String messageKey;

    /**
     * 语言环境（如：zh_CN, en_US）
     */
    private String locale;
    /**
     * 类型 1:后端 2:前端
     */
    private String type;
    /**
     * 消息内容
     */
    private String messageValue;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建者
     */
    private Long createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    private Long updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
