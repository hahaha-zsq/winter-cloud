package com.winter.cloud.auth.application.service;

import com.winter.cloud.auth.api.dto.response.MenuResponseDTO;

import java.util.List;

public interface AuthMenuAppService {
    List<MenuResponseDTO> getMenu(Long userId);

}