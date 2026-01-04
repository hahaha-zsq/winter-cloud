package com.winter.cloud.dict.domain.model.entity;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class DictTypeDO {
    private Long id;
    private String dictName;
    private String dictType;
    private String status;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
