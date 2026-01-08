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
public class LoginResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String token;
    private Long id;
    private String userName;
    private String nickName;
    private String avatar;
    private String email;
    private String phone;
    private String bgImg;
    private String sex;
    private String introduction;
    private MenuAndButtonResponseDTO menuAndButton;
}