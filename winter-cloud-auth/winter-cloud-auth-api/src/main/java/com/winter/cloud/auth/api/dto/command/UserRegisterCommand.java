package com.winter.cloud.auth.api.dto.command;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class UserRegisterCommand implements Serializable {
    private Long id;
    @NotBlank(message = "用户名不能为空")
    private String userName;
    @NotBlank(message = "密码不能为空")
    private String password;
    private String nickName;
    private String email;
    private String phone;
}