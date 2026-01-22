package com.winter.cloud.auth.api.dto.query;

import com.winter.cloud.common.response.PageAndOrderDTO;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public class RoleQuery extends PageAndOrderDTO {
    private Long id;
    private String roleKey;
    private String roleName;
    private String status;
}
