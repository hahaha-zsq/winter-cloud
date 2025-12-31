package com.winter.cloud.auth.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MenuAndButtonResponseDTO {
    List<MenuResponseDTO> menuList;
    List<String> buttonList;
}
