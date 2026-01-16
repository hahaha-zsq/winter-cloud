package com.winter.cloud.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum MenuTypeEnum {
    BUTTON("b", "按钮"),
    MENU("m", "菜单"),
    DIR("c", "目录")
    ;

    private String code;
    private String message;

}
