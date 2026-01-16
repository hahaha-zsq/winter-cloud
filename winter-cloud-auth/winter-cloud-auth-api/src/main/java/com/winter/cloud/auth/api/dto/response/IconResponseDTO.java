package com.winter.cloud.auth.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IconResponseDTO {
    private Long id;
    private String name;
    private String url;
    private String prefix;
    private List<IconValue> children;
}
