package com.winter.cloud.auth.domain.repository;


import com.winter.cloud.auth.api.dto.query.PostQuery;
import com.winter.cloud.auth.domain.model.entity.AuthPostDO;
import com.winter.cloud.common.response.PageDTO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 职位仓储接口 (面向领域)
 */
public interface AuthPostRepository {

    List<AuthPostDO> postDynamicQueryList(PostQuery postQuery);
    AuthPostDO postDynamicQuery(PostQuery postQuery);
    Boolean hasDuplicatePost(AuthPostDO aDo);

    PageDTO<AuthPostDO> postPage(PostQuery postQuery);

    Boolean postSave(AuthPostDO aDo);

    Boolean postUpdate(AuthPostDO aDo);

    Boolean postDelete(List<Long> postIdList);

    void postExportExcelTemplate(HttpServletResponse response);

    void postImportExcel(HttpServletResponse response, MultipartFile file) throws IOException;

    void postExportExcel(HttpServletResponse response, List<AuthPostDO> doList);
}