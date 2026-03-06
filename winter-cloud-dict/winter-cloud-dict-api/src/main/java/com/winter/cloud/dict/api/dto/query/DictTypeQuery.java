package com.winter.cloud.dict.api.dto.query;

import com.winter.cloud.common.response.PageAndOrderDTO;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public class DictTypeQuery extends PageAndOrderDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;

    private String dictName;
}
