package com.winter.cloud.dict.api.dto.command;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
import java.util.List;

@Data
public class UpsertDictDataCommand implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;

    private Integer dictSort;

    @NotBlank(message = "{UpsertDictDataCommand.DictDataItem.dictLabel.notBlank}", groups = {Save.class, Update.class})
    private String dictLabel;

    @NotBlank(message = "{UpsertDictDataCommand.DictDataItem.dictValue.notBlank}", groups = {Save.class, Update.class})
    private String dictValue;

    private String status;

    private String remark;

    private Long dictTypeId;

    public interface Save {
    }

    public interface Update {
    }

}
