package com.winter.cloud.auth.api.dto.query;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MenuQuery {
    private Long id;
    private String menuName;
    private String status;
    private String perms;
    private String frame;
    private String path;
    private String menuType;
    private String visible;
}
