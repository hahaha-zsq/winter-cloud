package com.winter.cloud.dict.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 字典数据持久化对象
 * 
 * @author zsq
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "sys_dict_data")
public class DictDataPO implements Serializable {
    
    /**
     * 字典编码
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 字典排序
     */
    @TableField(value = "dict_sort")
    private Integer dictSort;

    /**
     * 字典标签
     */
    @TableField(value = "dict_label")
    private String dictLabel;

    /**
     * 字典键值
     */
    @TableField(value = "dict_value")
    private String dictValue;

    /**
     * 字典类型ID
     */
    @TableField(value = "dict_type_id")
    private Long dictTypeId;

    /**
     * 状态（1正常 0停用）
     */
    @TableField(value = "status")
    private String status;

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

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    private static final long serialVersionUID = 1L;
}
