package com.winter.cloud.auth.api.dto.command;

import com.zsq.winter.validation.annotation.DynamicEnum;
import com.zsq.winter.validation.annotation.SpelValid;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serializable;

@Data
public class UpsertPostCommand implements Serializable {
    private static final long serialVersionUID = 1L;
    @SpelValid(
            value = "#this != null",
            message = "{UpsertPostCommand.edit.id.notNull}",
            groups = {UpsertPostCommand.Update.class}
    )
    @SpelValid(
            value = "#this == null",
            message = "{UpsertPostCommand.save.id.must.null}",
            groups = {UpsertPostCommand.Save.class}
    )
    private Long id;

    @SpelValid(
            value = "#this != null && !#this.trim().isEmpty()",
            message = "{UpsertPostCommand.postName.notEmpty}",
            groups = {UpsertPostCommand.Save.class, UpsertPostCommand.Update.class}
    )
    @Length(min = 1,max = 20, message = "{UpsertPostCommand.postName.length}", groups = {UpsertPostCommand.Save.class, UpsertPostCommand.Update.class})
    @SpelValid(
            value = "#this == null || #this matches '^[a-zA-Z0-9\\u4e00-\\u9fa5]+$'",
            message = "{UpsertPostCommand.postName.regex}",
            groups = {UpsertPostCommand.Save.class, UpsertPostCommand.Update.class}
    )
    private String postName;


    @SpelValid(
            value = "#this != null && !#this.trim().isEmpty()",
            message = "{UpsertPostCommand.postCode.notEmpty}",
            groups = {UpsertPostCommand.Save.class, UpsertPostCommand.Update.class}
    )
    @Length(min = 1,max = 30, message = "{UpsertPostCommand.postCode.length}", groups = {UpsertPostCommand.Save.class, UpsertPostCommand.Update.class})
    @SpelValid(
            value = "#this == null || #this matches '^[a-zA-Z0-9]+$'",
            message = "{UpsertPostCommand.postCode.regex}",
            groups = {UpsertPostCommand.Save.class, UpsertPostCommand.Update.class}
    )
    private String postCode;

    @DynamicEnum(
            dictType = "110",
            message = "{UpsertPostCommand.status.illegal}",
            groups ={UpsertPostCommand.Save.class, UpsertPostCommand.Update.class}
    )
    private String status;

    @SpelValid(
            value = "#this == null || #this.length() <= 200",
            message = "{UpsertPostCommand.remark.length}",
            groups = {UpsertPostCommand.Save.class, UpsertPostCommand.Update.class}
    )
    private String remark;

    @SpelValid(
            value = "#this != null",
            message = "{common.sort.notNull}",
            groups = {UpsertPostCommand.Save.class, UpsertPostCommand.Update.class}
    )
    private Integer orderNum;

    public interface Save {}
    public interface Update {}
}
