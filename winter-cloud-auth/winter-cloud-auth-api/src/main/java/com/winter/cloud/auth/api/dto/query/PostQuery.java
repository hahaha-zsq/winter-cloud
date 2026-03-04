package com.winter.cloud.auth.api.dto.query;

import com.winter.cloud.common.response.PageAndOrderDTO;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public class PostQuery extends PageAndOrderDTO {
//    职位名称
    private String postName;
//    职位编码
    private String postCode;
//    状态
    private String status;

    private Long id;

}
