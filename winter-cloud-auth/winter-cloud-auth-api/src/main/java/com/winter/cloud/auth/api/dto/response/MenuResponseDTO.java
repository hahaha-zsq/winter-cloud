package com.winter.cloud.auth.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
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
    private String ancestors;
    // 强制赋予默认值，且 Builder 会尊重这个默认值，使用了 @Builder 注解。Lombok 的 Builder 模式在构建对象时，如果不做特殊处理，会忽略字段定义的默认值
    @Builder.Default
    private List<MenuResponseDTO> children = new ArrayList<>();
}
