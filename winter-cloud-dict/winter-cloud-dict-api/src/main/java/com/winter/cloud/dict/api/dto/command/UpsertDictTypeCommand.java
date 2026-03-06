package com.winter.cloud.dict.api.dto.command;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;

@Data
public class UpsertDictTypeCommand implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "{UpsertDictTypeCommand.id.edit.notNull}", groups = {Update.class})
    @Null(message = "{UpsertDictTypeCommand.id.save.Null}", groups = {Save.class})
    private Long id;

    @NotBlank(message = "{UpsertDictTypeCommand.dictName.notBlank}", groups = {Save.class, Update.class})
    private String dictName;

    private String remark;

    public interface Save {}
    public interface Update {}
}
