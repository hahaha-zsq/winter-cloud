package com.winter.cloud.auth.application.service;


import com.winter.cloud.auth.api.dto.command.UpsertPostCommand;
import com.winter.cloud.auth.api.dto.query.PostQuery;
import com.winter.cloud.auth.api.dto.query.RoleQuery;
import com.winter.cloud.auth.api.dto.response.PostResponseDTO;
import com.winter.cloud.common.response.PageDTO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.List;

public interface AuthPostAppService {

    List<PostResponseDTO> postDynamicQueryList(PostQuery postQuery);

    PageDTO<PostResponseDTO> postPage(PostQuery postQuery);

    Boolean postSave(UpsertPostCommand command);

    Boolean postUpdate(UpsertPostCommand command);

    Boolean postDelete(@Valid @NotEmpty(message = "{delete.data.notEmpty}") List<Long> postIds);

    void postExportExcelTemplate(HttpServletResponse response);

    void postImportExcel(HttpServletResponse response, MultipartFile file) throws IOException;

    void postExportExcel(HttpServletResponse response, PostQuery postQuery);
}