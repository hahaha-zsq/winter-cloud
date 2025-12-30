package com.winter.cloud.auth.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

// LoginResponseDTO.java
@Data
@Builder
public class LoginResponseDTO implements Serializable {
    private String token;
    private Long userId;
    private String userName;
    private String nickName;
    // 可以包含 roleKeyList, permissionList 等
}