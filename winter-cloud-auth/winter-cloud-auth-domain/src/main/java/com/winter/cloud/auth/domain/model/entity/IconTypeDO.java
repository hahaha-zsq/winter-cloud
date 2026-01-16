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
public class IconTypeDO implements Serializable {
    private Long id;
    private String name;
    private String url;
    private String prefix;
}
