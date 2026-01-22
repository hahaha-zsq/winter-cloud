package com.winter.cloud.auth.api.dto.query;

import com.winter.cloud.common.response.PageAndOrderDTO;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public class PostQuery extends PageAndOrderDTO {
    private String postName;
    private  Long id;
    private String postCode;
    private String status;

}
