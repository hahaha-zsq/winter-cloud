package com.winter.cloud.auth.application.service;


import com.winter.cloud.auth.api.dto.response.IconResponseDTO;

import java.util.List;

public interface IconAppService {
    void insert(String name);

    List<IconResponseDTO> getIconList(String name);
}
