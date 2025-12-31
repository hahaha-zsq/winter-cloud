package com.winter.cloud.auth.domain.model.entity;

import com.zsq.winter.encrypt.util.CryptoUtil;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户领域实体
 * 注意：这里不依赖 MyBatis Plus 的任何注解，保持纯净
 */
@Data
public class AuthUserDO {
    private Long id;
    private String userName;
    private String nickName;
    private String email;
    private String phone;
    private String sex;
    private String avatar;
    private String password;
    private String status;
    private String delFlag;
    
    // 审计字段通常在领域层也可以保留，或者封装成 ValueObject
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
    
    private String remark;
    private Long postId;
    private String countryId;
    private String introduction;
    private String bgImg;

    // --- 可以添加领域行为方法 ---
    
    /**
     * 校验密码是否正确（用户传入的密码进行加密后和数据库中的数据进行比较）
     */
    public boolean verifyPassword(String inputPassword, String encryptedPassword) {
        return CryptoUtil.winterMd5Hex16(inputPassword).equals(encryptedPassword);
    }

    /**
     * 启用用户
     */
    public void enable() {
        this.status = "0";
    }
}