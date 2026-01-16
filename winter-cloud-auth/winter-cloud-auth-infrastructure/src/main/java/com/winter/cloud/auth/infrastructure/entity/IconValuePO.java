package com.winter.cloud.auth.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("sys_icon_value")
public class IconValuePO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     **/
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 图标名称
     **/
    @TableField(value = "value")
    private String value;
    /**
     * 图标类别
     **/
    @TableField(value = "icon_type_id")
    private Long iconTypeId;

    /**
     * 1:正常，0:禁用
     **/
    @TableField(value = "status")
    private String status;
}
