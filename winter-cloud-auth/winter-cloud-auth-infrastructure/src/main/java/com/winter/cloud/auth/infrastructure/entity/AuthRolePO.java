package com.winter.cloud.auth.infrastructure.entity;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.*;
import com.winter.cloud.auth.infrastructure.service.impl.excel.ExcelSelectImpl;
import com.zsq.winter.office.annotation.excel.WinterExcelSelected;
import com.zsq.winter.validation.annotation.DynamicEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色信息表
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "sys_role")
public class AuthRolePO implements Serializable {
    /**
     * 角色ID
     */
    @ExcelIgnore
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 角色名称
     */
    @ExcelProperty(value = {"角色信息","角色名称"})
    @TableField(value = "role_name")
    @NotBlank(message = "{UpsertRoleCommand.roleName.notEmpty}", groups = {AuthRolePO.Import.class})
    @Pattern(regexp = "^[\\u4e00-\\u9fa5]+$", message = "{common.language.chinese}", groups = {AuthRolePO.Import.class})
    @Length(min = 1, max = 20, message = "{UpsertRoleCommand.roleName.length}", groups = {AuthRolePO.Import.class})
    private String roleName;

    /**
     * 角色权限字符串
     */
    @ExcelProperty(value = {"角色信息","角色权限字符"})
    @TableField(value = "role_key")
    @NotBlank(message = "{UpsertRoleCommand.roleKey.notEmpty}", groups = {AuthRolePO.Import.class})
    @Pattern(regexp = "^[a-zA-Z]+$", message = "{common.language.english}", groups = {AuthRolePO.Import.class})
    @Length(min = 1, max = 30, message = "{UpsertRoleCommand.roleKey.length}", groups = {AuthRolePO.Import.class})
    private String roleKey;

    /**
     * 显示顺序
     */
    @ExcelProperty(value = {"角色信息","显示顺序"})
    @TableField(value = "role_sort")
    @NotNull(message = "{common.sort.notNull}", groups = {AuthRolePO.Import.class})
    private Integer roleSort;

    /**
     * 帐号状态（0正常 1停用）
     */
    @ExcelProperty(value = {"角色信息","角色状态"})
    @TableField(value = "status")
    @WinterExcelSelected(sourceClass = ExcelSelectImpl.class,firstRow = 2,type = "110")
    @DynamicEnum(
            dictType = "110",
            reverse = true,
            message = "{UpsertI18NCommand.type.illegal}",
            groups = {AuthRolePO.Import.class}
    )
    private String status;

    /**
     * 创建者
     */
    @ExcelIgnore
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 创建时间
     */
    @ExcelProperty(value = {"角色信息","创建时间"})
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

    /**
     * 备注
     */
    @ExcelProperty(value = {"角色信息","备注"})
    @TableField(value = "remark")
    @Size(max = 200, message = "{UpsertRoleCommand.remark.length}",groups ={AuthRolePO.Import.class})
    private String remark;

    public interface Import {}
    

    private static final long serialVersionUID = 1L;
}