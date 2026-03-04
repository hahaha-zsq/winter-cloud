package com.winter.cloud.auth.domain.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.winter.cloud.auth.api.dto.command.UserRegisterCommand;
import com.winter.cloud.auth.api.dto.query.UserQuery;
import com.winter.cloud.auth.api.dto.response.UserResponseDTO;
import com.winter.cloud.auth.domain.model.entity.AuthUserDO;
import com.winter.cloud.common.response.PageDTO;
import com.winter.cloud.common.response.Response;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 用户仓储接口 (面向领域)
 */
public interface AuthUserRepository {

    /**
     * 根据ID查询用户
     */
    AuthUserDO findById(Long id);

    /**
     * 根据用户名查询用户
     */
    AuthUserDO findByEmail(String email);

    /**
     * 保存或更新用户
     */
    Boolean save(AuthUserDO authUser);

    /**
     * 获取用户角色列表
     */
    List<String> getRoleKeyList(Long userId);


    /**
     * 删除用户
     */
    void deleteById(Long id);

    // 检查用户名是否存在
    boolean hasDuplicateUser(UserRegisterCommand command);

    PageDTO<AuthUserDO> userPage(UserQuery userQuery);

    Boolean userSave(AuthUserDO aDo);

    Boolean userUpdate(AuthUserDO aDo);

    Boolean userDelete(List<Long> id);

    Response<Boolean> updatePasswordBySuperMan(Long id, String password);

    void userExportExcelTemplate(HttpServletResponse response);

    void userImportExcel(HttpServletResponse response, MultipartFile file) throws IOException;

    void userExportExcel(HttpServletResponse response, List<UserResponseDTO> records);
}