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
    @SpelValid(
            value = "#this != null",
            message = "编辑用户，ID不能为空",
            groups = {Update.class}
    )
    @SpelValid(
            value = "#this != null",
            message = "重制密码时ID不能为空",
            groups = {ResetPassword.class}
    )
    @SpelValid(
            value = "#this = null",
            message = "新增用户，ID必须为空",
            groups = {Save.class}
    )
    private Long id;

    /**
     * 用户名校验
     */
    @SpelValid(
            value = "#this != null && !#this.trim().isEmpty()",
            message = "用户名不能为空",
            groups = {Save.class, Update.class}
    )
    @SpelValid(
            value = "#this == null || (#this.length() >= 3 && #this.length() <= 20)",
            message = "用户名长度需在3-20之间",
            groups = {Save.class, Update.class}
    )
    @SpelValid(
            value = "#this == null || #this matches '^[a-zA-Z0-9\\u4e00-\\u9fa5]+$'",
            message = "只允许字母、数字和中文",
            groups = {Save.class, Update.class}
    )
    private String userName;

    /**
     * 昵称校验
     */
    @SpelValid(
            value = "#this != null && !#this.trim().isEmpty()",
            message = "昵称不能为空",
            groups = {Save.class, Update.class}
    )
    @SpelValid(
            value = "#this == null || (#this.length() >= 1 && #this.length() <= 8)",
            message = "昵称长度必须在1到8个字符之间",
            groups = {Save.class, Update.class}
    )
    private String nickName;

    /**
     * 密码校验
     */
    @SpelValid(
            value = "#this != null && !#this.trim().isEmpty()",
            message = "密码不能为空",
            groups = {Save.class, Update.class, ResetPassword.class}
    )
    @SpelValid(
            value = "#this == null || #this matches '^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,15}$'",
            message = "密码长度必须为8-15位，且必须包含大小写字母、数字和特殊字符",
            groups = {Save.class, Update.class, ResetPassword.class}
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
            message = "性别类型非法！",
            allowNull = true,
            allowEmpty = true,
            groups = {Save.class, Update.class}
    )
    @SpelValid(
            value = "#this != null && !#this.trim().isEmpty()",
            message = "性别不能为空",
            groups = {Save.class, Update.class}
    )
    private String sex;

    /**
     * 手机号校验
     */
    @SpelValid(
            value = "#this != null && !#this.trim().isEmpty()",
            message = "手机号不能为空",
            groups = {Save.class, Update.class}
    )
    @SpelValid(
            value = "#this == null || #this matches '^1[3-9]\\d{9}$'",
            message = "手机号格式不正确",
            groups = {Save.class, Update.class}
    )
    private String phone;

    /**
     * 邮箱校验
     */
    @SpelValid(
            value = "#this != null && !#this.trim().isEmpty()",
            message = "邮箱不能为空",
            groups = {Save.class, Update.class}
    )
    @SpelValid(
            value = "#this == null || #this.length() <= 100",
            message = "邮箱长度不能超过100个字符",
            groups = {Save.class, Update.class}
    )
    @SpelValid(
            value = "#this == null || #this matches '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$'",
            message = "邮箱格式不正确",
            groups = {Save.class, Update.class}
    )
    private String email;

    /**
     * 职位编号校验
     */
    @SpelValid(
            value = "#this != null",
            message = "职位编号不能为空！",
            groups = {Save.class, Update.class}
    )
    private Long postId;
    /**
     * 角色编号集合校验
     */
    @SpelValid(
            value = "#this != null && !#this.isEmpty()",
            message = "角色编号集合不能为空！",
            groups = {Save.class, Update.class}
    )
    private List<Long> roleIds;
    /**
     * 部门编号集合校验
     */
    @SpelValid(
            value = "#this != null && !#this.isEmpty()",
            message = "部门编号集合不能为空！",
            groups = {Save.class, Update.class}
    )
    private List<Long> deptIds;


    /**
     * 状态校验
     */
    @SpelValid(
            value = "#this != null && !#this.trim().isEmpty()",
            message = "用户状态不能为空！",
            groups = {Save.class, Update.class}
    )
    @DynamicEnum(
            dictType = "110",
            message = "用户状态类型非法！",
            allowNull = true,
            allowEmpty = true,
            groups = {Save.class, Update.class}
    )
    private String status;

    /**
     * 简介校验
     */
    @SpelValid(
            value = "#this == null || #this.length() <= 200",
            message = "个人简介长度不能超过200字符",
            groups = {Save.class, Update.class}
    )
    private String introduction;


    private String bgImg;
    /**
     * 头像校验
     */
    @SpelValid(
            value = "#this != null && !#this.trim().isEmpty()",
            message = "头像不能为空",
            groups = {Save.class, Update.class}
    )
    @SpelValid(
            value = "#this == null || #this.length() <= 300",
            message = "头像链接长度不能超过300字符",
            groups = {Save.class, Update.class}
    )
    @SpelValid(
            value = "#this == null || #this matches '^https?://.+'",
            message = "头像必须是合法的URL链接格式(以http或https开头)",
            groups = {Save.class, Update.class}
    )
    private String avatar;


    public interface Save {
    }

    public interface Update {
    }

    public interface ResetPassword {

    }
}
