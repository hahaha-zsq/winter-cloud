package com.winter.cloud.auth.api.dto.command;

import com.zsq.winter.validation.annotation.SpelValid;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
@SpelValid(
        value = "#root.roleId != null",
        message = "分配资源时角色id不能为空"
)
@SpelValid(
        value = "#root.menuIds != null && #root.menuIds.size() > 0",
        message = "分配资源时资源不能为空"
)
@Data
public class AssignResourcesCommand implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long roleId;
    private List<Long> menuIds;
}
