package com.winter.cloud.auth.api.dto.command;

import com.zsq.winter.validation.annotation.SpelValid;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class UserLoginCommand implements Serializable {
    private static final long serialVersionUID = 1L;
    @NotNull(message = "邮箱不能为空")
    @SpelValid(
            value = "#this matches '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$'",
            message = "邮箱格式不正确"
    )
    private String email;
    /**
     * 密码校验：
     * 1. 长度 8-15
     * 2. 必须包含：大小写字母、数字、特殊字符
     */
    @NotNull(message = "密码不能为空")
    @SpelValid(
            // 逻辑：正则断言分别检查小写、大写、数字、特殊字符，总长度8-15
            // 注意：在Java字符串中，正则表达式的 \ 需要转义为 \\
            value = "#this matches '^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,15}$'",
            message = "密码长度需在8-15之间，且必须包含大小写字母、数字和特殊字符"
    )
    private String password;
}