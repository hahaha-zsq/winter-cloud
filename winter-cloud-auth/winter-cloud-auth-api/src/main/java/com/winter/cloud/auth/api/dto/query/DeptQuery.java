package com.winter.cloud.auth.api.dto.query;

import com.winter.cloud.common.response.PageAndOrderDTO;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public class DeptQuery extends PageAndOrderDTO {
    private String deptName;
    private  Long id;
    private Long parentId;
    private String status;

}
