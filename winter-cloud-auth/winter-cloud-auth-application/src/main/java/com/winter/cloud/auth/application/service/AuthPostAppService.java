package com.winter.cloud.auth.application.service;


import com.winter.cloud.auth.api.dto.response.PostResponseDTO;

import java.util.List;

public interface AuthPostAppService {


    List<PostResponseDTO> getAllPostInfo(String postName, String status);
}