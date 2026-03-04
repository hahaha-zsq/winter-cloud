package com.winter.cloud.auth.interfaces.controller;

import com.winter.cloud.auth.api.dto.command.UpsertPostCommand;
import com.winter.cloud.auth.api.dto.query.PostQuery;
import com.winter.cloud.auth.api.dto.response.PostResponseDTO;
import com.winter.cloud.auth.application.service.AuthPostAppService;
import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.response.PageDTO;
import com.winter.cloud.common.response.Response;
import com.zsq.i18n.template.WinterI18nTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.List;

/**
 * 职位管理控制层，提供职位相关的RESTful接口。
 * <p>
 * 包含职位的查询、新增、修改、删除、导入导出等功能。
 *
 * @author winter-cloud
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/post")
public class AuthPostController {
    private final AuthPostAppService authPostAppService;
    private final WinterI18nTemplate winterI18nTemplate;


    /**
     * 根据动态条件查询职位列表。
     * <p>
     * 支持根据职位状态、职位名称等条件进行模糊查询和动态过滤。
     *
     * @param postQuery 职位查询条件，包含分页信息和过滤条件
     * @return 符合条件的职位列表
     * @throws IllegalArgumentException 如果查询条件无效
     */
    @PostMapping("/postDynamicQueryList")
    public Response<List<PostResponseDTO>> postDynamicQueryList(@RequestBody @Validated PostQuery postQuery) {
        List<PostResponseDTO> data = authPostAppService.postDynamicQueryList(postQuery);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()), data);
    }

    /**
     * 分页查询职位列表。
     * <p>
     * 返回分页后的职位数据，包含总记录数等信息。
     *
     * @param postQuery 职位查询条件，包含分页信息和过滤条件
     * @return 分页后的职位列表
     * @throws IllegalArgumentException 如果分页参数无效
     */
    @PostMapping("/postPage")
    public Response<PageDTO<PostResponseDTO>> postPage(@RequestBody PostQuery postQuery) {
        PageDTO<PostResponseDTO> data = authPostAppService.postPage(postQuery);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()), data);
    }

    /**
     * 新增职位信息。
     * <p>
     * 创建一个新的职位记录，职位名称必须唯一。
     *
     * @param command 职位信息，包含职位名称、状态、排序等属性
     * @return 操作是否成功
     * @throws IllegalArgumentException 如果职位信息验证失败
     * @throws com.winter.cloud.common.exception.BusinessException 如果职位名称已存在
     */
    @PostMapping("/postSave")
    public Response<Boolean> postSave(@RequestBody @Validated(UpsertPostCommand.Save.class) UpsertPostCommand command) {
        Boolean data = authPostAppService.postSave(command);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()), data);
    }

    /**
     * 修改职位信息。
     * <p>
     * 更新指定职位的相关信息，职位名称必须唯一。
     *
     * @param command 职位信息，包含职位ID、名称、状态、排序等属性
     * @return 操作是否成功
     * @throws IllegalArgumentException 如果职位信息验证失败
     * @throws com.winter.cloud.common.exception.BusinessException 如果职位不存在或名称已存在
     */
    @PutMapping("/postUpdate")
    public Response<Boolean> postUpdate(@RequestBody @Validated(UpsertPostCommand.Update.class) UpsertPostCommand command) {
        Boolean data = authPostAppService.postUpdate(command);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()), data);
    }

    /**
     * 删除职位信息。
     * <p>
     * 批量删除指定的职位，支持软删除或硬删除。
     *
     * @param postIdList 职位ID集合，不能为空
     * @return 操作是否成功
     * @throws IllegalArgumentException 如果ID列表为空
     * @throws com.winter.cloud.common.exception.BusinessException 如果职位关联了用户
     */
    @DeleteMapping("/postDelete")
    public Response<Boolean> postDelete(@RequestBody @Valid @NotEmpty(message = "{delete.data.notEmpty}") List<Long> postIdList) {
        Boolean data = authPostAppService.postDelete(postIdList);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()), data);
    }

    /**
     * 导出职位数据到Excel文件。
     * <p>
     * 根据查询条件导出符合条件的职位数据为Excel格式。
     *
     * @param response  HTTP响应对象，用于设置响应头和输出流
     * @param postQuery 导出数据的查询条件
     * @throws IOException 如果写入Excel文件失败
     * @see #postExportExcelTemplate(HttpServletResponse)
     */
    @PostMapping(value = "/postExportExcel")
    public void postExportExcel(HttpServletResponse response, @RequestBody PostQuery postQuery) {
        authPostAppService.postExportExcel(response, postQuery);
    }

    /**
     * 导出职位Excel导入模板。
     * <p>
     * 下载用于批量导入职位的Excel模板文件。
     *
     * @param response HTTP响应对象，用于设置响应头和输出流
     * @throws IOException 如果写入Excel文件失败
     * @see #postExportExcel(HttpServletResponse, PostQuery)
     */
    @PostMapping(value = "/postExportExcelTemplate")
    public void postExportExcelTemplate(HttpServletResponse response) {
        authPostAppService.postExportExcelTemplate(response);
    }

    /**
     * 从Excel文件导入职位数据。
     * <p>
     * 解析Excel文件中的职位数据并进行批量导入。
     *
     * @param response HTTP响应对象，用于返回导入结果
     * @param file     Excel文件，包含职位数据
     * @throws IOException 如果读取Excel文件失败
     * @throws IllegalArgumentException 如果文件格式不正确
     */
    @PostMapping(value = "/postImportExcel")
    public void postImportExcel(HttpServletResponse response, @RequestParam(value = "file") MultipartFile file) throws IOException {
        authPostAppService.postImportExcel(response,file);
    }

}
