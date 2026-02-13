package com.winter.cloud.auth.api.dto.command;

import com.zsq.winter.validation.annotation.DynamicEnum;
import com.zsq.winter.validation.annotation.SpelValid;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.*;
import java.io.Serializable;

/**
 * 菜单新增/修改命令对象
 * 表达式逻辑结构为 A || B (A 或 B)。在校验场景中，这种写法通常用于表达 “如果...那么...” 的逻辑：
 * value = "!{'c', 'm'}.contains(#root.menuType) || (#root.icon != null && !#root.icon.isEmpty() && #root.icon.length() <= 60)",
 * 如果满足 A（即 menuType 不为 'c'，'m'），则表达式直接为 true，校验通过（跳过后面的检查）。
 * 如果不满足 A（即 menuType 恰好为 'b'），则必须满足 B，校验才能通过。
 */
@Data
@SpelValid(
        value = "!{'c', 'm'}.contains(#root.menuType) || (#root.icon != null && !#root.icon.isEmpty() && #root.icon.length() <= 60)",
        message = "当菜单类型为目录(m)或菜单(c)时，图标不能为空且长度不能超过60",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'b' || (#root.perms != null && !#root.perms.isEmpty() && #root.perms.length() <= 50)",
        message = "当菜单类型为按钮(b)时，权限编码不能为空且长度必须在[1,50]之间",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'c' || (#root.path != null && !#root.path.isEmpty() && #root.path.length() <= 120 && #root.path.matches('^/[a-zA-Z/]*$'))",
        message = "当菜单类型为菜单(c)时，路由地址不能为空，长度[1,120]，必须以/开头且只能包含英文或/",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'c' || (#root.component != null && !#root.component.isEmpty() && #root.component.length() <= 80 && #root.component.matches('^/[a-zA-Z/]*$'))",
        message = "当菜单类型为菜单(c)时，组件存放地址不能为空，长度[1,80]，必须以/开头且只能包含英文或/",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'c' || (#root.filePath != null && !#root.filePath.isEmpty() && #root.filePath.length() <= 40 && #root.filePath.matches('^[a-z][a-zA-Z]*\\.(tsx|jsx)$'))",
        message = "当资源类型为菜单(c)时，组件名称不能为空，长度[1,40]，必须是英文且首字母小写，并以.tsx或.jsx结尾",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
// 1. 目录 (c): 外链必须为空(null或"")；可见性、资源状态必须有值
@SpelValid(
        value = "#root.menuType != 'c' || ((#root.frame == null || #root.frame == '') && (#root.visible != null && #root.visible != ''&& #root.status != null && #root.status != ''))",
        message = "当资源类型为目录(c)时，外链必须为空(null或空字符)；可见性、资源状态必须有值",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
// 2. 菜单 (m): 外链、可见性、资源状态 都必须有值
@SpelValid(
        value = "#root.menuType != 'm' || (#root.frame != null && #root.frame == '' && #root.visible != null && #root.visible != '' && #root.status != null && #root.status != '')",
        message = "当资源类型为菜单(m)时，外链状态、可见性、资源状态必须有值,不可以是空字符",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
// 3. 按钮 (b): 资源状态必须有值；外链、可见性必须为空(null或"")
@SpelValid(
        value = "#root.menuType != 'b' || ((#root.status != null && #root.status != '') && (#root.frame == null || #root.frame == '') && (#root.visible == null || #root.visible == ''))",
        message = "当资源类型为按钮(b)时，资源状态必须有值,不可以是空字符，外链和可见性不允许有值",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
public class UpsertMenuCommand implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "{UpsertMenuCommand.id.edit.notNull}", groups = {Update.class})
    @Null(message = "{UpsertMenuCommand.id.save.Null}", groups = {Save.class})
    private Long id;

    @NotNull(groups = {Save.class, Update.class}, message = "上级资源编号不能为空！")
    private Long parentId;

    @NotBlank(groups = {Save.class, Update.class}, message = "资源名称不能为空！")
    @Length(min = 1, max = 20, message = "资源名称的长度[1,20]之间", groups = {Save.class, Update.class})
    @Pattern(groups = {Save.class, Update.class}, regexp = "^[\\u4e00-\\u9fa5a-zA-Z]+$", message = "只能输入中文或者英文")
    private String menuName;

    @NotNull(message = "排序号不能为空", groups = {Save.class, Update.class})
    private Integer orderNum;

    @DynamicEnum(
            dictType = "114",
            message = "{UpsertMenuCommand.menuType.illegal}",
            groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
    )
    private String menuType;

    // 修改：allowNull = true，以便 SpEL 根据 menuType 决定是否允许为 null
    @DynamicEnum(
            dictType = "110",
            allowNull = true,
            allowEmpty = true,
            message = "{UpsertMenuCommand.status.illegal}",
            groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
    )
    private String status;

    private String icon;

    // 修改：allowNull = true
    @DynamicEnum(
            dictType = "117",
            allowNull = true,
            allowEmpty = true,
            message = "{UpsertMenuCommand.visible.illegal}",
            groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
    )
    private String visible;

    private String perms;

    private String path;

    private String filePath;

    private String component;

    // 修改：allowNull = true
    @DynamicEnum(
            dictType = "118",
            allowNull = true,
            allowEmpty = true,
            message = "{UpsertMenuCommand.frame.illegal}",
            groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
    )
    private String frame;

    public interface Save {}
    public interface Update {}
}