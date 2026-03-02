package com.winter.cloud.auth.infrastructure.entity;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.*;
import com.winter.cloud.auth.infrastructure.service.impl.excel.ExcelSelectImpl;
import com.zsq.winter.office.annotation.excel.WinterExcelSelected;
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
    @ExcelProperty(value = {"用户信息","用户名称"})
    @TableField(value = "user_name")
    private String userName;

    /**
     * 用户昵称
     */
    @ExcelProperty(value = {"用户信息","用户昵称"})
    @TableField(value = "nick_name")
    private String nickName;

    /**
     * 用户邮箱
     */
    @ExcelProperty(value = {"用户信息","用户邮箱"})
    @TableField(value = "email")
    private String email;

    /**
     * 手机号码
     */
    @ExcelProperty(value = {"用户信息","手机号码"})
    @TableField(value = "phone")
    private String phone;

    /**
     * 用户性别（0男 1女 2未知）
     */
    @ExcelProperty(value = {"用户信息","用户性别"})
    @WinterExcelSelected(sourceClass = ExcelSelectImpl.class,firstRow = 2,type = "1")
    @TableField(value = "sex")
    private String sex;

    /**
     * 头像地址
     */
    @ExcelProperty(value = {"用户信息","头像地址"})
    @TableField(value = "avatar")
    private String avatar;

    /**
     * 密码
     */
    @ExcelProperty(value = {"用户信息","用户密码"})
    @TableField(value = "password")
    private String password;

    /**
     * 帐号状态（0正常 1停用）
     */
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
    @ExcelProperty(value = {"用户信息","职位名称"})
    @TableField(value = "post_id")
    private Long postId;

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






//    /**
//     * 扩展字段
//     */
//    @TableField(value = "ext_json")
//    private String extJson;
}
