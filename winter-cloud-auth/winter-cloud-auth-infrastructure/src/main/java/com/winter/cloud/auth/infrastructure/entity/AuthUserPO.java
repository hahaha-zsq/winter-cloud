package com.winter.cloud.auth.infrastructure.entity;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.*;
import com.winter.cloud.auth.infrastructure.service.impl.excel.ExcelSelectImpl;
import com.zsq.winter.office.annotation.excel.WinterExcelSelected;
import com.zsq.winter.validation.annotation.DynamicEnum;
import com.zsq.winter.validation.annotation.SpelValid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "sys_user")
public class AuthUserPO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @ExcelIgnore
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户账号
     */
    @SpelValid(
            value = "#this != null && !#this.trim().isEmpty()",
            message = "用户名不能为空",
            groups = {AuthUserPO.Import.class}
    )
    @SpelValid(
            value = "#this == null || (#this.length() >= 3 && #this.length() <= 20)",
            message = "用户名长度需在3-20之间",
            groups = {AuthUserPO.Import.class}
    )
    @SpelValid(
            value = "#this == null || #this matches '^[a-zA-Z0-9\\u4e00-\\u9fa5]+$'",
            message = "只允许字母、数字和中文",
            groups = {AuthUserPO.Import.class}
    )
    @ExcelProperty(value = {"用户信息","用户名称"})
    @TableField(value = "user_name")
    private String userName;

    /**
     * 用户昵称
     */
    @SpelValid(
            value = "#this != null && !#this.trim().isEmpty()",
            message = "昵称不能为空",
            groups = {AuthUserPO.Import.class}
    )
    @SpelValid(
            value = "#this == null || (#this.length() >= 1 && #this.length() <= 8)",
            message = "昵称长度必须在1到8个字符之间",
            groups = {AuthUserPO.Import.class}
    )
    @ExcelProperty(value = {"用户信息","用户昵称"})
    @TableField(value = "nick_name")
    private String nickName;

    /**
     * 用户邮箱
     */
    @SpelValid(
            value = "#this != null && !#this.trim().isEmpty()",
            message = "邮箱不能为空",
            groups = {AuthUserPO.Import.class}
    )
    @SpelValid(
            value = "#this == null || #this.length() <= 100",
            message = "邮箱长度不能超过100个字符",
            groups = {AuthUserPO.Import.class}
    )
    @SpelValid(
            value = "#this == null || #this matches '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$'",
            message = "邮箱格式不正确",
            groups = {AuthUserPO.Import.class}
    )
    @ExcelProperty(value = {"用户信息","用户邮箱"})
    @TableField(value = "email")
    private String email;

    /**
     * 手机号码
     */
    @SpelValid(
            value = "#this != null && !#this.trim().isEmpty()",
            message = "手机号不能为空",
            groups = {AuthUserPO.Import.class}
    )
    @SpelValid(
            value = "#this == null || #this matches '^1[3-9]\\d{9}$'",
            message = "手机号格式不正确",
            groups = {AuthUserPO.Import.class}
    )
    @ExcelProperty(value = {"用户信息","手机号码"})
    @TableField(value = "phone")
    private String phone;


    /**
     * 用户性别（0男 1女 2未知）
     */
    @DynamicEnum(
            dictType = "1",
            message = "性别类型非法！",
            reverse = true,
            groups = {AuthUserPO.Import.class}
    )
    @ExcelProperty(value = {"用户信息","用户性别"})
    @WinterExcelSelected(sourceClass = ExcelSelectImpl.class,firstRow = 2,type = "1")
    @TableField(value = "sex")
    private String sex;

    /**
     * 头像地址
     */
    @SpelValid(
            value = "#this != null && !#this.trim().isEmpty()",
            message = "头像不能为空",
            groups = {AuthUserPO.Import.class}
    )
    @SpelValid(
            value = "#this == null || #this.length() <= 300",
            message = "头像链接长度不能超过300字符",
            groups = {AuthUserPO.Import.class}
    )
    @SpelValid(
            value = "#this == null || #this matches '^https?://.+'",
            message = "头像必须是合法的URL链接格式(以http或https开头)",
            groups = {AuthUserPO.Import.class}
    )
    @ExcelProperty(value = {"用户信息","头像地址"})
    @TableField(value = "avatar")
    private String avatar;

    /**
     * 密码
     */
    @SpelValid(
            value = "#this != null && !#this.trim().isEmpty()",
            message = "密码不能为空",
            groups = {AuthUserPO.Import.class}
    )
    @SpelValid(
            value = "#this == null || #this matches '^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,15}$'",
            message = "密码长度必须为8-15位，且必须包含大小写字母、数字和特殊字符",
            groups = {AuthUserPO.Import.class}
    )
    @ExcelProperty(value = {"用户信息","用户密码"})
    @TableField(value = "password")
    private String password;

    /**
     * 帐号状态（0正常 1停用）
     */
    @DynamicEnum(
            dictType = "110",
            message = "用户状态类型非法！",
            reverse = true,
            groups = {AuthUserPO.Import.class}
    )
    @WinterExcelSelected(sourceClass = ExcelSelectImpl.class,firstRow = 2,type = "110")
    @ExcelProperty(value = {"用户信息","用户状态"})
    @TableField(value = "status")
    private String status;


    /**
     * 备注
     */
    @TableField(value = "remark")
    @ExcelProperty(value = {"用户信息","用户备注"})
    private String remark;

    /**
     * 职位id
     */
    @ExcelIgnore
    @TableField(value = "post_id")
    private Long postId;

    @SpelValid(
            value = "#this != null",
            message = "职位编号不能为空！",
            groups = {AuthUserPO.Import.class}
    )
    @TableField(exist = false)
    @ExcelProperty(value = {"用户信息","职位名称"})
    private String postName;

    /**
     * 简介
     */
    @ExcelProperty(value = {"用户信息","用户简介"})
    @TableField(value = "introduction")
    private String introduction;

    /**
     * 个人简介背景图
     */
    @ExcelProperty(value = {"用户信息","用户背景"})
    @TableField(value = "bg_img")
    private String bgImg;
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
