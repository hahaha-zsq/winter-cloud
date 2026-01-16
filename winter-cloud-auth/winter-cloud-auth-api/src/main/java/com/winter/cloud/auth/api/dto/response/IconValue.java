package com.winter.cloud.auth.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IconValue {
    private Long id;
    private String value;
    private Long iconTypeId;
    private String status;
}