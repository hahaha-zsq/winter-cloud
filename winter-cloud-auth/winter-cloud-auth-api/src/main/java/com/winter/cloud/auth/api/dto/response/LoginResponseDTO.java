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
    private Long userId;
    private String userName;
    private String nickName;
    private MenuAndButtonResponseDTO menuAndButton;
    // 可以包含 roleKeyList, permissionList 等
}