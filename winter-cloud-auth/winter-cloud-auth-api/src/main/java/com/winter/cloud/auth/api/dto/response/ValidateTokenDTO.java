package com.winter.cloud.auth.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Token 验证结果传输对象
 */
@Data
@Builder
public class ValidateTokenDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Token 是否有效
     */
    private Boolean valid;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String userName;
    
    /**
     * 角色列表
     */
    private List<String> roles;
    
    /**
     * 权限列表
     */
    private List<String> permissions;
    
    /**
     * 消息
     */
    private String message;
}
