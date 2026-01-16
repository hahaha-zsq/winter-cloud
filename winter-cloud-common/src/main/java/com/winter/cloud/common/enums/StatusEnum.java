package com.winter.cloud.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum StatusEnum {
    DISABLE("0", "禁用"),
    ENABLE("1", "启用"),
    ;

    private String code;
    private String message;

}
