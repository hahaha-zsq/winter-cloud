package com.winter.cloud.i18n.infrastructure.entity;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.*;
import com.winter.cloud.i18n.infrastructure.service.impl.excel.ExcelSelectImpl;
import com.zsq.winter.office.annotation.excel.WinterExcelSelected;
import com.zsq.winter.office.entity.excel.covert.LocalDateTimeConverter;
import com.zsq.winter.validation.annotation.DynamicEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 国际化消息实体
 *
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("i18n_messages")
public class I18nMessagePO {

    /**
     * 主键ID
     */
    @ExcelIgnore
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 消息键
     */
    @ExcelProperty(value = {"国际化","消息键"})
    @TableField("message_key")
    @NotBlank(message = "{UpsertI18NCommand.messageKey.notBlank}", groups = {I18nMessagePO.Import.class})
    @Length(min = 1, max = 80, message = "{UpsertI18NCommand.messageKey.length}", groups = {I18nMessagePO.Import.class})
    private String messageKey;

    /**
     * 语言环境（如：zh_CN, en_US）
     */
    @ExcelProperty(value = {"国际化","语言环境"})
    @WinterExcelSelected(sourceClass = ExcelSelectImpl.class,firstRow = 2,type = "115")
    @TableField("locale")
    @DynamicEnum(
            dictType = "115",
            reverse = true,
            message = "{UpsertI18NCommand.messageMap.locale.illegal}",
            groups = {I18nMessagePO.Import.class}
    )
    private String locale;

    /**
     * 消息内容
     */
    @ExcelProperty(value = {"国际化","消息内容"})
    @TableField("message_value")
    @NotBlank(message = "{UpsertI18NCommand.messageMap.messageValue.notBlank}", groups = {I18nMessagePO.Import.class})
    @Length(min = 1,max = 500, message = "{UpsertI18NCommand.messageMap.messageValue.length}", groups = {I18nMessagePO.Import.class})
    private String messageValue;

    /**
     * 描述
     */
    @ExcelProperty(value = {"国际化","描述"})
    @TableField("description")
    private String description;

    /**
     * 类型 1:后端 2:前端
     */
    @ExcelProperty(value = {"国际化","类型"})
    @WinterExcelSelected(sourceClass = ExcelSelectImpl.class,firstRow = 2,type = "116")
    @TableField("type")
    @DynamicEnum(
            dictType = "116",
            reverse = true,
            message = "{UpsertI18NCommand.type.illegal}",
            groups = {I18nMessagePO.Import.class}
    )
    private String type;

    /**
     * 创建者
     */
    @ExcelIgnore
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 创建时间
     */
    @ExcelProperty(value = {"国际化","创建时间"},converter = LocalDateTimeConverter.class)
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    @ExcelIgnore
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 更新时间
     */
    @ExcelIgnore
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public interface Import {}

}
