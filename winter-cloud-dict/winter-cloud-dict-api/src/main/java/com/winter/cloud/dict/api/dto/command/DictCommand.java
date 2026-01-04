package com.winter.cloud.dict.api.dto.command;

import lombok.Data;

import java.io.Serializable;

@Data
public class DictCommand implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 字典类型
     */
    private Long dictType;
    /**
     * 字典类型状态
     */
    private String status;
}
