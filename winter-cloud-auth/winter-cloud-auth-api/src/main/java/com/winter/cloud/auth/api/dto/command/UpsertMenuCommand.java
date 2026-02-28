package com.winter.cloud.auth.api.dto.command;

import com.zsq.winter.validation.annotation.DynamicEnum;
import com.zsq.winter.validation.annotation.SpelValid;
import lombok.Data;

import java.io.Serializable;

/**
 * 菜单新增/修改命令对象
 * <p>
 * 变更说明：
 * 1. [目录] 强制要求 path, component, filePath 必须为空。
 * 2. 保持全量使用 @SpelValid 进行校验。
 * </p>
 */
@Data
// =================================================================================
// 0. 全局 ID 校验
// =================================================================================
@SpelValid(
        value = "#root.id == null",
        message = "新增时主键必须为空",
        groups = {UpsertMenuCommand.Save.class}
)
@SpelValid(
        value = "#root.id != null",
        message = "修改时主键不能为空",
        groups = {UpsertMenuCommand.Update.class}
)

// =================================================================================
// 一、 目录 (Directory - 'c') 校验规则
// =================================================================================

// [目录] 1. 基础字段
@SpelValid(
        value = "#root.menuType != 'c' || (#root.parentId != null)",
        message = "当类型为目录(c)时，上级资源编号不能为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'c' || (#root.orderNum != null)",
        message = "当类型为目录(c)时，排序号不能为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)

// [目录] 2. 资源名称
@SpelValid(
        value = "#root.menuType != 'c' || (#root.menuName != null && !#root.menuName.trim().isEmpty())",
        message = "当类型为目录(c)时，资源名称不能为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'c' || (#root.menuName == null || (#root.menuName.length() >= 1 && #root.menuName.length() <= 20))",
        message = "当类型为目录(c)时，资源名称长度必须在[1,20]之间",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'c' || (#root.menuName == null || #root.menuName.matches('^[\\u4e00-\\u9fa5a-zA-Z]+$'))",
        message = "当类型为目录(c)时，资源名称只能包含中文或英文",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)

// [目录] 3. 禁用字段 (Path, Component, FilePath，frame，perms) - 目录必须为空
@SpelValid(
        value = "#root.menuType != 'c' || (#root.path == null || #root.path == '')",
        message = "当类型为目录(c)时，路由地址必须为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'c' || (#root.perms == null || #root.perms == '')",
        message = "当类型为目录(c)时，权限标识必须为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'c' || (#root.component == null || #root.component == '')",
        message = "当类型为目录(c)时，组件名称必须为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'c' || (#root.filePath == null || #root.filePath == '')",
        message = "当类型为目录(c)时，组件文件路径必须为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'c' || (#root.frame == null || #root.frame == '')",
        message = "当类型为目录(c)时，外链必须为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
// [目录] 4. 图标
@SpelValid(
        value = "#root.menuType != 'c' || (#root.icon != null && !#root.icon.trim().isEmpty())",
        message = "当类型为目录(c)时，图标不能为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'c' || (#root.icon == null || #root.icon.length() <= 60)",
        message = "当类型为目录(c)时，图标长度不能超过60",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)

// [目录] 5. 可见性 & 状态
@SpelValid(
        value = "#root.menuType != 'c' || (#root.visible != null && !#root.visible.trim().isEmpty())",
        message = "当类型为目录(c)时，可见性不能为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'c' || (#root.status != null && !#root.status.trim().isEmpty())",
        message = "当类型为目录(c)时，资源状态不能为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)


// =================================================================================
// 二、 菜单 (Menu - 'm') 校验规则
// =================================================================================

// [菜单] 1. 基础字段
@SpelValid(
        value = "#root.menuType != 'm' || (#root.parentId != null)",
        message = "当类型为菜单(m)时，上级资源编号不能为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'm' || (#root.orderNum != null)",
        message = "当类型为菜单(m)时，排序号不能为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)

// [菜单] 2. 资源名称
@SpelValid(
        value = "#root.menuType != 'm' || (#root.menuName != null && !#root.menuName.trim().isEmpty())",
        message = "当类型为菜单(m)时，资源名称不能为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'm' || (#root.menuName == null || (#root.menuName.length() >= 1 && #root.menuName.length() <= 20))",
        message = "当类型为菜单(m)时，资源名称长度必须在[1,20]之间",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'm' || (#root.menuName == null || #root.menuName.matches('^[\\u4e00-\\u9fa5a-zA-Z]+$'))",
        message = "当类型为菜单(m)时，资源名称只能包含中文或英文",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)

// [菜单] 3. 路由地址
@SpelValid(
        value = "#root.menuType != 'm' || (#root.path != null && !#root.path.trim().isEmpty())",
        message = "当类型为菜单(m)时，路由地址不能为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'm' || (#root.path == null || #root.path.length() <= 120)",
        message = "当类型为菜单(m)时，路由地址长度不能超过120",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
// 3.1 内链菜单 (非外链) -> / 开头
@SpelValid(
        value = "(#root.menuType != 'm' || #root.frame == '1') || (#root.path == null || #root.path.matches('^/[a-zA-Z0-9/_-]*$'))",
        message = "当类型为菜单(m)且非外链时，路由地址必须以 / 开头，只包含字母、数字、下划线 _、短横线 -、斜杠 / 的路径字符串",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
// 3.2 外链菜单 (是外链) -> http(s) 开头
@SpelValid(
        value = "(#root.menuType != 'm' || #root.frame != '1') || (#root.path != null && (#root.path.startsWith('http://') || #root.path.startsWith('https://')))",
        message = "当类型为菜单(m)且为外链时，路由地址必须以http://或https://开头",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)

// [菜单] 4. 组件配置
// 4.1 内链菜单：必填
@SpelValid(
        value = "(#root.menuType != 'm' || #root.frame == '1') || (#root.component != null && !#root.component.trim().isEmpty())",
        message = "当类型为菜单(m)且非外链时，组件名称不能为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "(#root.menuType != 'm' || #root.frame == '1') || (#root.filePath != null && !#root.filePath.trim().isEmpty())",
        message = "当类型为菜单(m)且非外链时，组件文件路径不能为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
// 4.2 外链菜单：必须为空
// 4.2 外链菜单：组件名称必须为空
@SpelValid(
        value = "!(#root.menuType == 'm' && #root.frame == '1') || (#root.component == null || #root.component == '')",
        message = "当类型为菜单(m)且为外链时，组件名称必须为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
// 4.2 外链菜单：文件路径必须为空
@SpelValid(
        value = "!(#root.menuType == 'm' && #root.frame == '1') || (#root.filePath == null || #root.filePath == '')",
        message = "当类型为菜单(m)且为外链时，组件文件路径必须为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
// 4.3 长度与格式
// 1. 组件名称长度校验
@SpelValid(
        // 逻辑：(不是菜单 OR 是外链) || (满足长度要求)
        value = "(#root.menuType != 'm' || #root.frame == '1') || (#root.component == null || #root.component.length() <= 40)",
        message = "当类型为菜单(m)且非外链时，组件名称长度不能超过40",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)

// 2. 组件名称格式校验 (.tsx/.jsx)
@SpelValid(
        // 逻辑：(不是菜单 OR 是外链) || (满足格式要求)
        value = "(#root.menuType != 'm' || #root.frame == '1') || (#root.component == null || #root.component.matches('^[a-z][a-zA-Z]*\\.(tsx|jsx)$'))",
        message = "当类型为菜单(m)且非外链时，组件名称必须以.tsx或.jsx结尾",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)

// 3. 组件文件路径长度校验
@SpelValid(
        // 逻辑：(不是菜单 OR 是外链) || (满足长度要求)
        value = "(#root.menuType != 'm' || #root.frame == '1') || (#root.filePath == null || #root.filePath.length() <= 80)",
        message = "当类型为菜单(m)且非外链时，组件文件路径长度不能超过80",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)

// 4. 组件文件路径格式校验 (/开头)
@SpelValid(
        // 逻辑：(不是菜单 OR 是外链) || (满足格式要求)
        value = "(#root.menuType != 'm' || #root.frame == '1') || (#root.filePath == null || #root.filePath.matches('^/[a-zA-Z0-9/_-]*$'))",
        message = "当类型为菜单(m)且非外链时，组件文件路径必须以 / 开头，只包含字母、数字、下划线 _、短横线 -、斜杠 / 的路径字符串",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
// [菜单] 5. 图标
@SpelValid(
        value = "#root.menuType != 'm' || (#root.icon != null && !#root.icon.trim().isEmpty())",
        message = "当类型为菜单(m)时，图标不能为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'm' || (#root.icon == null || #root.icon.length() <= 60)",
        message = "当类型为菜单(m)时，图标长度不能超过60",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)

// [菜单] 6. 可见性 & 状态 & 外链
@SpelValid(
        value = "#root.menuType != 'm' || (#root.visible != null && !#root.visible.trim().isEmpty())",
        message = "当类型为菜单(m)时，可见性不能为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'm' || (#root.status != null && !#root.status.trim().isEmpty())",
        message = "当类型为菜单(m)时，资源状态不能为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'm' || (#root.frame != null && !#root.frame.trim().isEmpty())",
        message = "当类型为菜单(m)时，外链选项不能为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)

// =================================================================================
// 三、 按钮 (Button - 'b') 校验规则
// =================================================================================

// [按钮] 1. 基础字段
@SpelValid(
        value = "#root.menuType != 'b' || (#root.parentId != null)",
        message = "当类型为按钮(b)时，上级资源编号不能为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'b' || (#root.orderNum != null)",
        message = "当类型为按钮(b)时，排序号不能为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)

// [按钮] 2. 资源名称
@SpelValid(
        value = "#root.menuType != 'b' || (#root.menuName != null && !#root.menuName.trim().isEmpty())",
        message = "当类型为按钮(b)时，资源名称不能为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'b' || (#root.menuName == null || (#root.menuName.length() >= 1 && #root.menuName.length() <= 20))",
        message = "当类型为按钮(b)时，资源名称长度必须在[1,20]之间",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'b' || (#root.menuName == null || #root.menuName.matches('^[\\u4e00-\\u9fa5a-zA-Z]+$'))",
        message = "当类型为按钮(b)时，资源名称只能包含中文或英文",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)

// [按钮] 3. 权限标识
@SpelValid(
        value = "#root.menuType != 'b' || (#root.perms != null && !#root.perms.trim().isEmpty())",
        message = "当类型为按钮(b)时，权限标识不能为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'b' || (#root.perms == null || #root.perms.length() <= 50)",
        message = "当类型为按钮(b)时，权限标识长度不能超过50",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)

// [按钮] 4. 状态
@SpelValid(
        value = "#root.menuType != 'b' || (#root.status != null && !#root.status.trim().isEmpty())",
        message = "当类型为按钮(b)时，资源状态不能为空",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)

// [按钮] 5. 禁用字段 (必须为空)
@SpelValid(
        value = "#root.menuType != 'b' || (#root.frame == null || #root.frame == '')",
        message = "当类型为按钮(b)时，外链不允许有值",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'b' || (#root.visible == null || #root.visible == '')",
        message = "当类型为按钮(b)时，可见性不允许有值",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'b' || (#root.path == null || #root.path == '')",
        message = "当类型为按钮(b)时，路由地址不允许有值",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'b' || (#root.component == null || #root.component == '')",
        message = "当类型为按钮(b)时，组件名称不允许有值",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'b' || (#root.filePath == null || #root.filePath == '')",
        message = "当类型为按钮(b)时，文件路径地址不允许有值",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)
@SpelValid(
        value = "#root.menuType != 'b' || (#root.icon == null || #root.icon == '')",
        message = "当类型为按钮(b)时，图标不允许有值",
        groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
)

public class UpsertMenuCommand implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long parentId;

    private String menuName;

    private Integer orderNum;

    @DynamicEnum(
            dictType = "114",
            message = "{UpsertMenuCommand.menuType.illegal}",
            groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
    )
    private String menuType; // c:目录 m:菜单 b:按钮

    @DynamicEnum(
            dictType = "110",
            allowNull = true,
            allowEmpty = true,
            message = "{UpsertMenuCommand.status.illegal}",
            groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
    )
    private String status; // 1正常 0禁用

    private String icon;

    @DynamicEnum(
            dictType = "117",
            allowNull = true,
            allowEmpty = true,
            message = "{UpsertMenuCommand.visible.illegal}",
            groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
    )
    private String visible; // 0隐藏 1显示

    private String perms;

    private String path;

    private String filePath;

    private String component;

    private String ancestors;

    @DynamicEnum(
            dictType = "118",
            allowNull = true,
            allowEmpty = true,
            message = "{UpsertMenuCommand.frame.illegal}",
            groups = {UpsertMenuCommand.Save.class, UpsertMenuCommand.Update.class}
    )
    private String frame; // 0否 1是

    public interface Save {}
    public interface Update {}
}