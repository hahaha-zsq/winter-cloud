package com.winter.cloud.auth.api.dto.command;

import com.zsq.winter.validation.annotation.SpelValid;
import lombok.Data;

import java.io.Serializable;

@Data
public class UpsertDeptCommand implements Serializable {
    private static final long serialVersionUID = 1L;
    @SpelValid(
            value = "#this != null",
            message = "{UpsertPostCommand.edit.id.notNull}",
            groups = {UpsertDeptCommand.Update.class}
    )
    @SpelValid(
            value = "#this == null",
            message = "{UpsertPostCommand.save.id.must.null}",
            groups = {UpsertDeptCommand.Save.class}
    )
    private Long id;

    private Long parentId;

    private String deptName;

    private Integer orderNum;

    private String status;

    private String remark;

    public interface Save {}
    public interface Update {}
}
