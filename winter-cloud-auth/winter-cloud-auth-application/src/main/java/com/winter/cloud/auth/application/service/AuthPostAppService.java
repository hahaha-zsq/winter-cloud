package com.winter.cloud.auth.application.service;


import com.winter.cloud.auth.api.dto.query.PostQuery;
import com.winter.cloud.auth.api.dto.response.PostResponseDTO;

import java.util.List;

public interface AuthPostAppService {

    List<PostResponseDTO> postDynamicQueryList(PostQuery postQuery);
}