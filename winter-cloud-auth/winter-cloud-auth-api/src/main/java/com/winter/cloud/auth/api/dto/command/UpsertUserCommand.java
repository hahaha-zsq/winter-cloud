package com.winter.cloud.auth.api.dto.command;

import com.zsq.winter.validation.annotation.DynamicEnum;
import com.zsq.winter.validation.annotation.SpelValid;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
public class UpsertUserCommand implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @NotNull(message = "编辑用户,ID不能为空", groups ={Update.class})
    private Long id;

    /**
     * 用户名校验：
     * 1. 长度 5-20
     * 2. 不能包含特殊字符 (只允许字母和数字)
     */
    @NotNull(message = "{UserRegisterCommand.username.notBlank}", groups ={Save.class, Update.class})
    @SpelValid(
            // 逻辑：长度在5-20之间 且 仅包含字母和数字
            value = "#this.length() >= 5 && #this.length() <= 20 && #this matches '^[a-zA-Z0-9]+$'",
            message = "用户名长度需在5-20之间，且只能包含字母和数字",
            groups ={Save.class, Update.class}
    )
    private String userName;

    /**
     * 昵称校验：
     * 1. 必须不为空 (@NotNull)
     * 2. 长度在 1 到 8 之间
     */
    @NotNull(message = "{UserRegisterCommand.nickname.notBlank}", groups ={Save.class, Update.class})
    @SpelValid(
            // 直接调用 String.length() 方法
            value = "#this.length() >= 1 && #this.length() <= 8",
            message = "昵称长度必须在1到8个字符之间",
            groups ={Save.class, Update.class}
    )
    private String nickName;


    /**
     * 密码校验：
     * 1. 长度 8-15
     * 2. 必须包含：大小写字母、数字、特殊字符
     */
    @NotNull(message = "{UserRegisterCommand.password.notBlank}", groups ={Save.class, Update.class})
    @SpelValid(
            // 逻辑：正则断言分别检查小写、大写、数字、特殊字符，总长度8-15
            // 注意：在Java字符串中，正则表达式的 \ 需要转义为 \\
            value = "#this matches '^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,15}$'",
            message = "{UserRegisterCommand.password.pattern}",
            groups ={Save.class, Update.class}
    )
    private String password;

    /**
     * 性别校验
     * 1. dictType 是必须参数，随便填一个标识即可（如 "gender"）
     * 2. fixedValues 定义允许的值列表
     * 3. allowNull = false 表示必填（默认就是 false）
     */
    @DynamicEnum(
            dictType = "1",
            message = "性别类型非法！"
    )
    private String sex;


    /**
     * 校验手机号 (中国大陆 11 位)
     * 1. 必须不为空 (@NotNull)
     * 2. 正则：以 1 开头，第二位是 3-9，后面接 9 位数字
     */
    @NotNull(message = "{UserRegisterCommand.phone.notBlank}", groups ={Save.class, Update.class})
    @SpelValid(
            value = "#this matches '^1[3-9]\\d{9}$'",
            message = "手机号格式不正确",
            groups ={Save.class, Update.class}
    )
    private String phone;


    /**
     * 校验邮箱
     * 1. 必须不为空 (@NotNull)
     * 2. 正则：标准的邮箱格式匹配
     * 注意：Java 字符串中反斜杠需转义，所以是 \\. 而不是 \.
     * ^ [a-zA-Z0-9._%+-]+   @   [a-zA-Z0-9.-]+   \\.   [a-zA-Z]{2,} $
     * | -----------------   |   --------------   ---   ------------ |
     * 开始      用户名        分隔       域名主名       点      域名后缀    结束
     */
    @NotNull(message = "{UserRegisterCommand.email.notBlank}",groups ={Save.class, Update.class})
    @SpelValid(
            value = "#this matches '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$'",
            message = "邮箱格式不正确",
            groups ={Save.class, Update.class}
    )
    private String email;


    @NotNull(message = "职位编号不能为空！", groups ={Save.class, Update.class})
    private Long postId;

    @NotEmpty(message = "角色编号集合不能为空！", groups ={Save.class, Update.class})
    private List<Long> roleIds;

    @NotEmpty(message = "部门编号集合不能为空！", groups ={Save.class, Update.class})
    private List<Long> deptIds;

    @DynamicEnum(
            dictType = "110",
            message = "用户状态类型非法！",
            groups ={Save.class, Update.class}
    )
    private String status;


    private String introduction;

    private String bgImg;




    public interface Save {
    }

    public interface Update {
    }
}
