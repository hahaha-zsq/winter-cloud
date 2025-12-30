package com.winter.cloud.auth.domain.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuthRoleDO {
    private Long id;
    private String roleKey;
    private String roleName;
    private Integer roleSort;
    private String status;
    private String remark;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
