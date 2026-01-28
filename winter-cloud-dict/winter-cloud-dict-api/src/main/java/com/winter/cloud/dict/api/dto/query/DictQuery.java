package com.winter.cloud.dict.api.dto.query;

import com.winter.cloud.common.response.PageAndOrderDTO;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public class DictQuery extends PageAndOrderDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 字典类型
     */
    private Long dictTypeId;
    /**
     * 字典值状态
     */
    private String status;
    /**
     * 字典标签
     */
    private String dictLabel;
    /**
     * 字典值
     */
    private String dictValue;
}
