package com.winter.cloud.dict.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 字典数据 DTO
 * 用于 RPC 接口返回
 * 
 * @author zsq
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictDataDTO implements Serializable {
    
    /**
     * 字典编码
     */
    private Long id;
    
    /**
     * 字典排序
     */
    private Integer dictSort;
    
    /**
     * 字典标签
     */
    private String dictLabel;
    
    /**
     * 字典键值
     */
    private String dictValue;
    
    /**
     * 状态（1正常 0停用）
     */
    private String status;
    
    /**
     * 字典类型ID
     */
    private Long dictTypeId;

    /**
     * 备注
     */
    private String remark;
    
    private static final long serialVersionUID = 1L;
}
