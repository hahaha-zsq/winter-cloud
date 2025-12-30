package com.winter.cloud.auth.api.dto.command;

import lombok.Data;

import java.io.Serializable;

@Data
public class RoleCommand implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String roleKey;
    private String roleName;
    private Integer roleSort;
    private String status;
    private String remark;
}
