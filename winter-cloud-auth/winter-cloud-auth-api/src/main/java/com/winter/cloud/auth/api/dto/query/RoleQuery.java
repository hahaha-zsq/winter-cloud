package com.winter.cloud.auth.api.dto.query;

import com.winter.cloud.common.response.PageAndOrderDTO;

public class RoleQuery extends PageAndOrderDTO {
    private String roleKey;
    private String roleName;
    private String status;
}
