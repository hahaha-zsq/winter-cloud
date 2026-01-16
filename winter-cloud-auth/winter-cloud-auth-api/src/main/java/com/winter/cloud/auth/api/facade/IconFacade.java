package com.winter.cloud.auth.api.facade;

import com.winter.cloud.auth.api.dto.response.IconResponseDTO;
import com.winter.cloud.common.response.Response;

import java.util.List;

public interface IconFacade {
    Response<List<IconResponseDTO>> getIconList(String name);
}
