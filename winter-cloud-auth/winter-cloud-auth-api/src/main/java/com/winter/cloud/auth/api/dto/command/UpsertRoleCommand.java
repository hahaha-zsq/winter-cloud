package com.winter.cloud.auth.api.dto.command;

import com.zsq.winter.validation.annotation.DynamicEnum;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.*;
import java.io.Serializable;

@Data
public class UpsertRoleCommand implements Serializable {
    private static final long serialVersionUID = 1L;
    @NotNull(message = "{UpsertRoleCommand.id.edit.notNull}", groups = {Update.class})
    @Null(message = "{UpsertRoleCommand.id.save.Null}", groups = {Save.class})
    private Long id;

    @NotBlank(message = "{UpsertRoleCommand.roleKey.notEmpty}", groups = {UpsertRoleCommand.Save.class, UpsertRoleCommand.Update.class})
    @Pattern(regexp = "^[a-zA-Z]+$", message = "{common.language.english}", groups = {UpsertRoleCommand.Save.class, UpsertRoleCommand.Update.class})
    @Length(min = 1, max = 30, message = "{UpsertRoleCommand.roleKey.length}", groups = {UpsertRoleCommand.Save.class, UpsertRoleCommand.Update.class})
    private String roleKey;

    @NotBlank(message = "{UpsertRoleCommand.roleName.notEmpty}", groups = {UpsertRoleCommand.Save.class, UpsertRoleCommand.Update.class})
    @Pattern(regexp = "^[\\u4e00-\\u9fa5]+$", message = "{common.language.chinese}", groups = {UpsertRoleCommand.Save.class, UpsertRoleCommand.Update.class})
    @Length(min = 1, max = 20, message = "{UpsertRoleCommand.roleName.length}", groups = {UpsertRoleCommand.Save.class, UpsertRoleCommand.Update.class})
    private String roleName;

    @NotNull(message = "{common.sort.notNull}", groups = {UpsertRoleCommand.Save.class, UpsertRoleCommand.Update.class})
    private Integer roleSort;

    @DynamicEnum(
            dictType = "110",
            message = "{UpsertRoleCommand.status.illegal}",
            groups ={UpsertRoleCommand.Save.class, UpsertRoleCommand.Update.class}
    )
    private String status;

    @Size(max = 200, message = "{UpsertRoleCommand.remark.length}")
    private String remark;

    public interface Save {}
    public interface Update {}
}
