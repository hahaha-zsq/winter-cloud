package com.winter.cloud.auth.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class RoleResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String roleKey;
    private String roleName;
    private Integer roleSort;
    private String status;
    private String remark;
}
