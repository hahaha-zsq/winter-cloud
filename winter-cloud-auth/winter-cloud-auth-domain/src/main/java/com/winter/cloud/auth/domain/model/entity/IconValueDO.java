package com.winter.cloud.auth.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class IconValueDO implements Serializable {
    private Long id;
    private String value;
    private Long iconTypeId;
    private String status;
}
