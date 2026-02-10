package com.winter.cloud.i18n.api.dto.command;

import com.zsq.winter.validation.annotation.DynamicEnum;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
import java.util.List;

@Data
public class UpsertI18NCommand implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "{UpsertI18NCommand.id.edit.notNull}", groups = {Update.class})
    @Null(message = "{UpsertI18NCommand.id.save.Null}", groups = {Save.class})
    private Long id;

    @NotBlank(message = "{UpsertI18NCommand.messageKey.notBlank}", groups = {Save.class, Update.class})
    @Length(min = 1, max = 80, message = "{UpsertI18NCommand.messageKey.length}", groups = {Save.class, Update.class})
    private String messageKey;

    private String description;

    @DynamicEnum(
            dictType = "116",
            message = "{UpsertI18NCommand.type.illegal}",
            groups = {Save.class, Update.class}
    )
    private String type;

    @Valid
    @NotEmpty(
            message = "{UpsertI18NCommand.messageValueList.notEmpty}",
            groups = {Save.class, Update.class}
    )
    private List<MessageMap> messageValueList;

    @Data
    public static class MessageMap {
        @DynamicEnum(
                dictType = "115",
                message = "{UpsertI18NCommand.messageMap.locale.illegal}",
                groups = {Save.class, Update.class}
        )
        private String locale;

        @NotBlank(message = "{UpsertI18NCommand.messageMap.messageValue.notBlank}", groups = {Save.class, Update.class})
        @Length(min = 1,max = 500, message = "{UpsertI18NCommand.messageMap.messageValue.length}", groups = {Save.class, Update.class})
        private String messageValue;
    }

    public interface Save {}
    public interface Update {}
}