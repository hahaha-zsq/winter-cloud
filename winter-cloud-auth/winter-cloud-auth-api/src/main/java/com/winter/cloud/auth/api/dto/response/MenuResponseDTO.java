package com.winter.cloud.auth.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MenuResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long parentId;
    private String menuName;
    private String perms;
    private Integer orderNum;
    private String path;
    private String filePath;
    private String component;
    private String menuType;
    private String frame;
    private String visible;
    private String status;
    private String icon;
}
