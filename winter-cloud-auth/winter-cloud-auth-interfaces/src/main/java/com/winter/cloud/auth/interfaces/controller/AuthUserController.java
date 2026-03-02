package com.winter.cloud.auth.interfaces.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.winter.cloud.auth.api.dto.command.UpsertUserCommand;
import com.winter.cloud.auth.api.dto.command.UserLoginCommand;
import com.winter.cloud.auth.api.dto.command.UserRegisterCommand;
import com.winter.cloud.auth.api.dto.query.UserQuery;
import com.winter.cloud.auth.api.dto.response.LoginResponseDTO;
import com.winter.cloud.auth.api.dto.response.UserResponseDTO;
import com.winter.cloud.auth.api.dto.response.ValidateTokenDTO;
import com.winter.cloud.auth.api.facade.AuthValidationFacade;
import com.winter.cloud.auth.application.service.AuthUserAppService;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.exception.BusinessException;
import com.winter.cloud.common.response.PageDTO;
import com.winter.cloud.common.response.Response;
import com.winter.cloud.common.util.JwtUtil;
import com.zsq.i18n.template.WinterI18nTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.List;


/**
 * 认证用户控制器
 * <p>
 * DDD 架构中的 Interface 层（接口层），负责处理 HTTP 协议的请求与响应。
 * 本控制器提供用户注册、登录、token 验证、用户管理（增删改查）等功能，
 * 供网关通过 HTTP 协议调用。
 * </p>
 * <p>
 * 主要功能模块：
 * <ul>
 *   <li>用户注册 - 用户注册新账号</li>
 *   <li>用户登录 - 用户登录系统并获取 token</li>
 *   <li>Token 验证 - 验证 JWT token 有效性，供网关鉴权使用</li>
 *   <li>用户管理 - 用户的增删改查操作</li>
 *   <li>密码管理 - 管理员重置用户密码</li>
 * </ul>
 * </p>
 *
 * @author winter
 * @since 1.0.0
 * @see AuthValidationFacade
 * @see AuthUserAppService
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@DubboService
@Validated
@RequestMapping("/auth")
public class AuthUserController implements AuthValidationFacade {
    private final AuthUserAppService authUserAppService;
    private final WinterI18nTemplate winterI18nTemplate;

    /**
     * 验证 Token 有效性并返回用户权限信息
     * <p>
     * 该接口用于网关层调用，验证用户请求中携带的 JWT Token 是否有效，
     * 并返回用户的身份信息、角色和权限列表，供网关进行鉴权判断。
     * </p>
     * 
     * <p>验证流程：</p>
     * <ol>
     *   <li>校验 Token 是否为空</li>
     *   <li>校验 Token 格式是否正确及是否过期</li>
     *   <li>解析 Token 获取用户 ID 和用户名</li>
     *   <li>获取用户的角色和权限信息</li>
     *   <li>构建并返回验证结果</li>
     * </ol>
     *
     * @param token JWT Token 字符串
     * @return ValidateTokenDTO Token 验证结果，包含有效性、用户信息、角色和权限
     */
    @Override
    public ValidateTokenDTO validateToken(String token) {
        log.info("=== Token 验证接口调用 ===");
        log.info("接收到 Token: {}", token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null");

        try {
            // 步骤1: 校验 Token 是否为空
            if (!StringUtils.hasText(token)) {
                log.warn("Token 验证失败: Token 为空");
                return buildFailureResult("Token 为空");
            }

            // 步骤2: 校验 Token 格式和有效期
            if (!JwtUtil.validateToken(token)) {
                log.warn("Token 验证失败: Token 无效或已过期");
                return buildFailureResult("Token 无效或已过期");
            }

            // 步骤3: 解析 Token 获取用户基本信息
            String subject = JwtUtil.getSubject(token);

            // 校验 Token 解析结果
            if (!StringUtils.hasText(subject)) {
                log.warn("Token 验证失败: 无法解析出用户 ID");
                return buildFailureResult("Token 解析失败");
            }

            String userName = (String) JwtUtil.getClaim(token, CommonConstants.Claim.NAME);

            if (!StringUtils.hasText(userName)) {
                log.warn("Token 验证失败: 获取用户名失败");
                return buildFailureResult("Token 解析失败");
            }

            // 转换用户 ID
            Long userId;
            try {
                userId = Long.parseLong(subject);
            } catch (NumberFormatException e) {
                log.error("Token 验证失败: 用户 ID 格式错误, subject: {}", subject);
                return buildFailureResult("Token 中用户 ID 格式错误");
            }

            // 步骤4: 获取用户角色和权限信息
            // 委托给应用服务层处理，获取完整的用户权限信息
            ValidateTokenDTO result = authUserAppService.generateUserInfo(userId, userName);

            // 设置验证成功消息
            result.setMessage("Token 有效");

            log.info("Token 验证成功 - 用户ID: {}, 用户名: {}, 角色数: {}, 权限数: {}", 
                    userId, userName, 
                    result.getRoles() != null ? result.getRoles().size() : 0,
                    result.getPermissions() != null ? result.getPermissions().size() : 0);
            return result;

        } catch (NumberFormatException e) {
            // 数字转换异常（用户 ID 解析失败）
            log.error("Token 验证失败: 用户 ID 解析异常", e);
            return buildFailureResult("Token 中用户 ID 格式错误");
        } catch (Exception e) {
            // 捕获所有未预期的异常，确保接口稳定性
            log.error("Token 验证过程发生异常", e);
            return buildFailureResult("Token 验证异常: " + e.getMessage());
        }
    }

    /**
     * 构建 Token 验证失败的返回结果
     *
     * @param message 失败原因描述
     * @return ValidateTokenDTO 失败结果对象，包含有效标识为 false 及失败消息
     */
    private ValidateTokenDTO buildFailureResult(String message) {
        return ValidateTokenDTO.builder()
                .valid(false)
                .message(message)
                .build();
    }


    /**
     * 用户注册接口
     * <p>
     * 处理用户注册请求，包括以下验证：
     * <ul>
     *   <li>用户名：长度5-20位，只能包含字母和数字</li>
     *   <li>密码：长度8-15位，必须包含大小写字母、数字和特殊字符</li>
     *   <li>手机号：中国大陆11位手机号</li>
     *   <li>邮箱：标准邮箱格式</li>
     *   <li>昵称：长度1-8位</li>
     * </ul>
     * </p>
     * <p>
     * 注册成功后，用户初始状态为正常（status="0"），默认不删除（del_flag="0"）。
     * </p>
     *
     * @param command 注册命令对象，包含用户名、密码、手机号、邮箱等必填信息
     * @return Response&lt;Boolean&gt; 注册结果，成功返回 true
     * @throws BusinessException 注册失败时抛出业务异常
     */
    @PostMapping("/register")
    public Response<Boolean> register(@RequestBody @Validated UserRegisterCommand command) {
        Boolean register = authUserAppService.register(command);
        if (!register) {
            return Response.fail(ResultCodeEnum.FAIL_LANG.getCode(),winterI18nTemplate.message(ResultCodeEnum.FAIL_LANG.getMessage()));
        }
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(),winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()),null);
    }

    /**
     * 用户登录接口
     * <p>
     * 处理用户登录请求，验证用户名和密码是否匹配。
     * 登录成功后返回 JWT Token 和用户基本信息，供后续请求鉴权使用。
     * </p>
     * <p>
     * 登录验证流程：
     * <ol>
     *   <li>校验用户名和密码格式</li>
     *   <li>查询用户是否存在且状态正常</li>
     *   <li>验证密码是否正确（MD5加密后比对）</li>
     *   <li>生成 JWT Token</li>
     *   <li>返回登录结果和 Token</li>
     * </ol>
     * </p>
     *
     * @param command 登录命令对象，包含用户名和密码
     * @return Response&lt;LoginResponseDTO&gt; 登录结果，包含 JWT Token 和用户信息
     * @throws JsonProcessingException JSON 序列化异常
     * @throws BusinessException 登录失败时抛出业务异常（用户不存在、密码错误、账号禁用等）
     */
    @PostMapping("/login")
    public Response<LoginResponseDTO> login(@RequestBody @Validated UserLoginCommand command) throws JsonProcessingException {
        LoginResponseDTO loginDTO = authUserAppService.login(command);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(),winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()),loginDTO);
    }


    /**
     * 分页查询用户列表
     * <p>
     * 支持多种查询条件的组合筛选，返回分页后的用户列表。
     * </p>
     * <p>
     * 查询条件说明：
     * <ul>
     *   <li><b>模糊查询</b>：用户名称、用户昵称</li>
     *   <li><b>精确查询</b>：手机号、邮箱、性别、状态、职位</li>
     *   <li><b>严格匹配</b>：部门、角色（如果选择了多个，用户必须同时拥有这些部门/角色）</li>
     *   <li><b>结果增强</b>：返回的部门信息包含父子层级结构</li>
     * </ul>
     * </p>
     *
     * @param userQuery 查询条件对象，支持分页参数（pageNum, pageSize）和各种筛选条件
     * @return Response&lt;PageDTO&lt;UserResponseDTO&gt;&gt; 分页结果，包含用户列表和总数
     */
    @PostMapping("/userPage")
    public Response<PageDTO<UserResponseDTO>> userPage(@RequestBody UserQuery userQuery) {
        PageDTO<UserResponseDTO> data = authUserAppService.userPage(userQuery);
        return Response.ok(
                ResultCodeEnum.SUCCESS_LANG.getCode(),
                winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()),
                data
        );
    }


    /**
     * 新增用户
     * <p>
     * 创建新的用户记录，包括用户基本信息、角色关联和部门关联。
     * </p>
     * <p>
     * 处理逻辑：
     * <ol>
     *   <li>校验用户名、手机号、邮箱是否与现有用户重复</li>
     *   <li>对密码进行 MD5 加密存储</li>
     *   <li>保存用户基本信息</li>
     *   <li>批量保存用户角色关联</li>
     *   <li>批量保存用户部门关联</li>
     * </ol>
     * </p>
     *
     * @param upsertUserCommand 用户命令对象，包含用户信息和角色/部门ID列表
     *                         使用 {@link UpsertUserCommand.Save} 分组进行参数校验
     * @return Response&lt;Boolean&gt; 操作结果，成功返回 true
     */
    @PostMapping("/userSave")
    public Response<Boolean> userSave(@RequestBody @Validated(UpsertUserCommand.Save.class) UpsertUserCommand upsertUserCommand) {
        Boolean data = authUserAppService.userSave(upsertUserCommand);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(),winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()),data);
    }

    /**
     * 更新用户信息
     * <p>
     * 更新指定用户的全部信息，包括基本信息、角色关联和部门关联。
     * 采用"先删后增"的策略重新维护关联关系。
     * </p>
     * <p>
     * 处理逻辑：
     * <ol>
     *   <li>校验用户名、手机号、邮箱是否与除当前用户外的其他用户重复</li>
     *   <li>更新用户基本信息（不包括密码，如需更新密码请使用专用接口）</li>
     *   <li>删除原有角色关联，新增新的角色关联</li>
     *   <li>删除原有部门关联，新增新的部门关联</li>
     * </ol>
     * </p>
     *
     * @param upsertUserCommand 用户命令对象，包含用户ID和更新后的信息
     *                         使用 {@link UpsertUserCommand.Update} 分组进行参数校验
     * @return Response&lt;Boolean&gt; 操作结果，成功返回 true
     * @throws BusinessException 更新失败时抛出业务异常（用户不存在、重复校验失败等）
     */
    @PutMapping("/userUpdate")
    public Response<Boolean> userUpdate(@RequestBody @Validated(UpsertUserCommand.Update.class) UpsertUserCommand upsertUserCommand) {
        Boolean data = authUserAppService.userUpdate(upsertUserCommand);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(),winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()),data);
    }

    /**
     * 批量删除用户
     * <p>
     * 物理删除指定的用户及其所有关联数据。删除操作包括：
     * <ol>
     *   <li>删除用户角色关联信息</li>
     *   <li>删除用户部门关联信息</li>
     *   <li>删除用户基本信息</li>
     * </ol>
     * </p>
     * <p>
     * 注意：删除顺序很重要，必须先删除关联数据，再删除用户本身，
     * 否则会导致关联数据无法通过 userId 查询到而清理不干净。
     * </p>
     *
     * @param idList 用户ID列表，支持批量删除，不能为空
     * @return Response&lt;Boolean&gt; 操作结果，成功返回 true
     * @throws BusinessException 删除失败时抛出业务异常
     */
    @DeleteMapping("/userDelete")
    public Response<Boolean> userDelete(@RequestBody @Valid @NotEmpty(message = "{delete.data.notEmpty}") List<Long> idList) {
        Boolean data = authUserAppService.userDelete(idList);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(),winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()),data);
    }

    /**
     * 超级管理员重置用户密码
     * <p>
     * 允许超级管理员强制重置指定用户的密码，无需验证原密码。
     * </p>
     * <p>
     * 注意：
     * <ul>
     *   <li>此接口仅限具有超级管理员权限的用户调用</li>
     *   <li>新密码需要符合密码强度要求（8-15位，包含大小写字母、数字和特殊字符）</li>
     *   <li>重置后密码会进行 MD5 加密存储</li>
     * </ul>
     * </p>
     *
     * @param upsertUserCommand 包含用户ID和新密码的命令对象
     *                         使用 {@link UpsertUserCommand.ResetPassword} 分组进行参数校验
     * @return Response&lt;Boolean&gt; 操作结果，成功返回 true
     * @throws BusinessException 重置失败时抛出业务异常（用户不存在、密码强度不足等）
     */
    @PutMapping("/updatePasswordBySuperMan")
    public Response<Boolean> updatePasswordBySuperMan(@RequestBody @Validated(UpsertUserCommand.ResetPassword.class) UpsertUserCommand upsertUserCommand) {
        return authUserAppService.updatePasswordBySuperMan(upsertUserCommand.getId(), upsertUserCommand.getPassword());
    }


    /**
     * 导出excel
     *
     * @param response 响应
     */
    @PostMapping(value = "/userExportExcel")
    public void userExportExcel(HttpServletResponse response) {
        authUserAppService.userExportExcel(response);
    }

    /**
     * 导出excel模板
     *
     * @param response 响应
     */
    @PostMapping(value = "/userExportExcelTemplate")
    public void userExportExcelTemplate(HttpServletResponse response) {
        authUserAppService.userExportExcelTemplate(response);
    }

    /**
     * 导入excel
     *
     * @param response 响应
     * @param file     文件
     */
    @PostMapping(value = "/userImportExcel")
    public void userImportExcel(HttpServletResponse response, @RequestParam(value = "file") MultipartFile file) throws IOException {
        authUserAppService.userImportExcel(response,file);
    }



}
