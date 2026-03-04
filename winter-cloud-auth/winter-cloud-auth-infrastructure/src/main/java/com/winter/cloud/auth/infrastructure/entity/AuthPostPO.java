package com.winter.cloud.auth.infrastructure.entity;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.*;

import com.winter.cloud.auth.api.dto.command.UpsertPostCommand;
import com.winter.cloud.auth.infrastructure.service.impl.excel.ExcelSelectImpl;
import com.zsq.winter.office.annotation.excel.WinterExcelSelected;
import com.zsq.winter.validation.annotation.DynamicEnum;
import com.zsq.winter.validation.annotation.SpelValid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.io.Serializable;
import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_post")
public class AuthPostPO implements Serializable {

    private static final long serialVersionUID = 1L;
    @ExcelIgnore
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @SpelValid(
            value = "#this != null && !#this.trim().isEmpty()",
            message = "{UpsertPostCommand.postCode.notEmpty}",
            groups = {UpsertPostCommand.Save.class, UpsertPostCommand.Update.class}
    )
    @Length(min = 1,max = 30, message = "{UpsertPostCommand.postCode.length}", groups = {UpsertPostCommand.Save.class, UpsertPostCommand.Update.class})
    @SpelValid(
            value = "#this == null || #this matches '^[a-zA-Z]+$'",
            message = "{UpsertPostCommand.postCode.regex}",
            groups = {UpsertPostCommand.Save.class, UpsertPostCommand.Update.class}
    )
    @ExcelProperty(value = {"职位信息","职位编码"})
    @TableField("post_code")
    private String postCode;

    @SpelValid(
            value = "#this != null && !#this.trim().isEmpty()",
            message = "{UpsertPostCommand.postName.notEmpty}",
            groups = {UpsertPostCommand.Save.class, UpsertPostCommand.Update.class}
    )
    @Length(min = 1,max = 20, message = "{UpsertPostCommand.postName.length}", groups = {UpsertPostCommand.Save.class, UpsertPostCommand.Update.class})
    @SpelValid(
            value = "#this == null || #this matches '^[a-zA-Z0-9\\u4e00-\\u9fa5]+$'",
            message = "{UpsertPostCommand.postName.regex}",
            groups = {UpsertPostCommand.Save.class, UpsertPostCommand.Update.class}
    )
    @ExcelProperty(value = {"职位信息","职位名称"})
    @TableField("post_name")
    private String postName;

    @SpelValid(
            value = "#this != null",
            message = "{common.sort.notNull}",
            groups = {UpsertPostCommand.Save.class, UpsertPostCommand.Update.class}
    )
    @ExcelProperty(value = {"职位信息","排序"})
    @TableField("order_num")
    private Integer orderNum;



    @DynamicEnum(
            dictType = "110",
            message = "{UpsertPostCommand.status.illegal}",
            reverse = true,
            groups = {AuthPostPO.Import.class}
    )
    @WinterExcelSelected(sourceClass = ExcelSelectImpl.class,firstRow = 2,type = "110")
    @ExcelProperty(value = {"职位信息","状态"})
    @TableField("status")
    private String status;

    /**
     * 备注
     */
    @SpelValid(
            value = "#this == null || #this.length() <= 200",
            message = "{UpsertPostCommand.remark.length}",
            groups = {AuthPostPO.Import.class}
    )
    @ExcelProperty(value = {"职位信息","备注"})
    @TableField(value = "remark")
    private String remark;

    /**
     * 创建者
     */
    @ExcelIgnore
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 创建时间
     */
    @ExcelIgnore
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
