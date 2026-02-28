package com.winter.cloud.auth.api.dto.command;

import com.zsq.winter.validation.annotation.DynamicEnum;
import com.zsq.winter.validation.annotation.SpelValid;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
@Builder
@Data
public class UserRegisterCommand implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 用户ID
     */
    @Null(message = "注册时用户ID必须为空")
    private Long id;
    /**
     * 用户名校验：
     * 1. 长度 5-20
     * 2. 不能包含特殊字符 (只允许字母和数字)
     */
    @NotNull(message = "用户名不能为空")
    @SpelValid(
            // 逻辑：长度在5-20之间 且 仅包含字母和数字
            value = "#this.length() >= 5 && #this.length() <= 20 && #this matches '^[a-zA-Z0-9]+$'",
            message = "用户名长度需在5-20之间，且只能包含字母和数字"
    )
    private String userName;

    /**
     * 密码校验：
     * 1. 长度 8-15
     * 2. 必须包含：大小写字母、数字、特殊字符
     */
    @NotNull(message = "{UserRegisterCommand.password.notBlank}")
    @SpelValid(
            // 逻辑：正则断言分别检查小写、大写、数字、特殊字符，总长度8-15
            // 注意：在Java字符串中，正则表达式的 \ 需要转义为 \\
            value = "#this matches '^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,15}$'",
            message = "{UserRegisterCommand.password.pattern}"
    )
    private String password;

    /**
     * 昵称校验：
     * 1. 必须不为空 (@NotNull)
     * 2. 长度在 1 到 8 之间
     */
    @NotNull(message = "昵称不能为空")
    @SpelValid(
            // 直接调用 String.length() 方法
            value = "#this.length() >= 1 && #this.length() <= 8",
            message = "昵称长度必须在1到8个字符之间"
    )
    private String nickName;

    /**
     * 校验手机号 (中国大陆 11 位)
     * 1. 必须不为空 (@NotNull)
     * 2. 正则：以 1 开头，第二位是 3-9，后面接 9 位数字
     */
    @NotNull(message = "手机号不能为空")
    @SpelValid(
            value = "#this matches '^1[3-9]\\d{9}$'",
            message = "手机号格式不正确"
    )
    private String phone;

    /**
     * 校验邮箱
     * 1. 必须不为空 (@NotNull)
     * 2. 正则：标准的邮箱格式匹配
     * 注意：Java 字符串中反斜杠需转义，所以是 \\. 而不是 \.
     * ^ [a-zA-Z0-9._%+-]+   @   [a-zA-Z0-9.-]+   \\.   [a-zA-Z]{2,} $
     *   | -----------------   |   --------------   ---   ------------ |
     * 开始      用户名        分隔       域名主名       点      域名后缀    结束
     */
    @NotNull(message = "邮箱不能为空")
    @SpelValid(
            value = "#this matches '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$'",
            message = "邮箱格式不正确"
    )
    private String email;

    /**
     * 性别校验
     * 1. dictType 是必须参数，随便填一个标识即可（如 "gender"）
     * 2. fixedValues 定义允许的值列表
     * 3. allowNull = false 表示必填（默认就是 false）
     */
    @DynamicEnum(
            dictType = "1",
            fixedValues = {"MALE", "FEMALE", "UNKNOWN"},
            message = "性别必须是 MALE, FEMALE 或 UNKNOWN"
    )
    private String sex;
}