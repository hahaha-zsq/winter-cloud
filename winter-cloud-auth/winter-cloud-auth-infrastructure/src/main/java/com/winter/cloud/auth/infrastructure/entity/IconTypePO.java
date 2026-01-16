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
@TableName("sys_icon_type")
public class IconTypePO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     **/
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 图标类型名称
     **/
    @TableField(value = "name")
    private String name;
    /**
     * 图标主页地址
     **/
    @TableField(value = "url")
    private String url;
    /**
     * 图标前缀
     **/
    @TableField(value = "prefix")
    private String prefix;
}
