package com.winter.cloud.auth.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜单表
 */
@Data
@Builder
@TableName(value = "sys_menu")
public class AuthMenuPO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 父级菜单ID
     */
    @TableField(value = "parent_id")
    private Long parentId;

    /**
     * 菜单名
     */
    @TableField(value = "menu_name")
    private String menuName;

    /**
     * 权限标识
     */
    @TableField(value = "perms")
    private String perms;

    /**
     * 排序，1在最上方
     */
    @TableField(value = "order_num")
    private Integer orderNum;

    /**
     * 路由地址
     */
    @TableField(value = "path")
    private String path;

    /**
     * 组件所在路径
     */
    @TableField(value = "file_path")
    private String filePath;

    /**
     * 组件名称
     */
    @TableField(value = "component")
    private String component;

    /**
     * 类型  c:目录   m：菜单   b：按钮
     */
    @TableField(value = "menu_type")
    private String menuType;

    /**
     * 是否为外链(0否 1是)
     */
    @TableField(value = "frame")
    private String frame;

    /**
     * 菜单是否显示（0隐藏 1显示）
     */
    @TableField(value = "visible")
    private String visible;

    /**
     * 菜单状态（1正常 0:禁用）
     */
    @TableField(value = "status")
    private String status;

    /**
     * 菜单图标
     */
    @TableField(value = "icon")
    private String icon;

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