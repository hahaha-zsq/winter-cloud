package com.winter.cloud.i18n.api.dto.command;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
public class UpsertI18NCommand implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @NotNull(message = "编辑用户,ID不能为空", groups ={Update.class})
    private Long id;

    private String messageKey;
    private String description;
    private String type;
    private List<MessageMap> messageValueList;



    @Data
    public static class MessageMap {
        private String locale;
        private String messageValue;
    }

    public interface Save {
    }

    public interface Update {
    }
}
