package com.winter.cloud.auth.api.dto.query;

import com.winter.cloud.common.response.PageAndOrderDTO;

import java.util.List;

public class UserQuery extends PageAndOrderDTO {
    private List<Long> deptIds;
    private String email;
    private String nickName;
    private String phone;
    private Long postId;
    private List<Long>  roleIds;
    private String sex;
    private String status;
    private String username;
}
