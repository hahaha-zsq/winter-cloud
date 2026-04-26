package com.winter.cloud.dict.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
