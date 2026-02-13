package com.winter.cloud.auth.api.dto.query;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MenuQuery {
    private Long id;
    private String menuName;
    // 只能是0，1
    private String status;
    private String perms;
    // 只能是0，1

    private String frame;
    private String path;
    // 只能是0，1

    private String menuType;
    // 只能是0，1

    private String visible;
}
