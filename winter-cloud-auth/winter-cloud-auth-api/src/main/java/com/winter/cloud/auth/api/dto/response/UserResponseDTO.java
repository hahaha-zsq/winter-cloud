package com.winter.cloud.auth.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDTO implements Serializable {
    private Long id;
    private String userName;
    private String nickName;
    private String email;
    private String phone;
    private String sex;
    private String avatar;
    private String status;
    private String remark;
    private String introduction;
    private String bgImg;
    private PostResponseDTO postDTO;
    private List<DeptResponseDTO> deptListDTO;
    private List<RoleResponseDTO> roleListDTO;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
