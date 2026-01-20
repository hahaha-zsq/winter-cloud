package com.winter.cloud.auth.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String postCode;
    private String postName;
    private Integer orderNum;
    private String status;
}
