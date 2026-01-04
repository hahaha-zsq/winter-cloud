package com.winter.cloud.dict.domain.model.entity;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class DictDataDO {
    private Long id;
    private Integer dictSort;
    private String dictLabel;
    private String dictValue;
    private Long dictTypeId;
    private String status;
    private String remark;
    private String dictName;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
