package com.winter.cloud.auth.domain.model.entity;

import com.zsq.winter.encrypt.util.CryptoUtil;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户领域实体
 * 注意：这里不依赖 MyBatis Plus 的任何注解，保持纯净
 */
@Data
public class AuthMenuDO {
    private Long id;
    private Long parentId;
    private String menuName;
    private String perms;
    private Integer orderNum;
    private String path;
    private String filePath;
    private String component;
    private String menuType;
    private String frame;
    private String visible;
    private String status;
    private String icon;
    
    // 审计字段通常在领域层也可以保留，或者封装成 ValueObject
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;


}