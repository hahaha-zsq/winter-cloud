package com.winter.cloud.i18n.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 国际化消息实体
 *
 */
@Data
@TableName("i18n_messages")
public class I18nMessagePO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 消息键
     */
    @TableField("message_key")
    private String messageKey;

    /**
     * 语言环境（如：zh_CN, en_US）
     */
    @TableField("locale")
    private String locale;

    /**
     * 消息内容
     */
    @TableField("message_value")
    private String messageValue;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * 类型 1:后端 2:前端
     */
    @TableField("type")
    private String type;

    /**
     * 创建者
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
