package com.winter.cloud.file.interfaces.controller;

import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.response.Response;
import com.winter.cloud.file.api.dto.query.FileUploadQuery;
import com.winter.cloud.file.api.dto.response.FileCheckDTO;
import com.winter.cloud.file.api.facade.FileFacade;
import com.winter.cloud.file.application.service.FileAppService;
import com.winter.cloud.i18n.api.facade.I18nMessageFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 文件服务控制器
 * <p>
 * 提供统一的文件上传与管理接口，对外暴露 HTTP 服务，同时作为 Dubbo 服务提供者。
 * 主要功能包括：
 * 1. 普通文件的单文件与批量上传（适用于小文件）。
 * 2. 大文件的分片上传全流程（检查/秒传、初始化、分片上传、合并、取消）。
 * 3. 集成国际化消息返回。
 *
 * @author winter
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@DubboService
@RequestMapping("/file")
public class FileController implements FileFacade {

    /**
     * 文件应用层服务，处理核心上传业务逻辑
     */
    private final FileAppService fileAppService;

    /**
     * 国际化消息服务（Dubbo 远程调用），用于获取多语言响应信息
     */
    @DubboReference(check = false)
    private I18nMessageFacade i18nMessageFacade;

    /**
     * 简单文件上传接口
     * <p>
     * 适用于不涉及分片逻辑的小文件直接上传。上传成功后直接返回文件访问 URL。
     *
     * @param file 前端传递的二进制文件对象 (MultipartFile)
     * @return Response&lt;String&gt; 包含文件访问网关 URL 的统一响应对象
     * @throws IOException 当文件流读取失败或上传过程中发生 IO 错误时抛出
     */
    @PostMapping("/uploadFile")
    public Response<String> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        // 调用应用服务执行上传
        String url = fileAppService.uploadFile(file);
        // 构建成功响应，使用国际化消息
        return Response.ok(
                ResultCodeEnum.SUCCESS_LANG.getCode(),
                i18nMessageFacade.getMessage(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()),
                url
        );
    }

    /**
     * 批量简单文件上传接口
     * <p>
     * 适用于批量上传小文件。会遍历列表依次上传，并返回对应的 URL 列表。
     *
     * @param files 前端传递的文件列表
     * @return Response&lt;List&lt;String&gt;&gt; 包含所有上传成功文件 URL 的列表
     */
    @PostMapping("/uploadFileList")
    public Response<List<String>> uploadFileList(@RequestParam("files") List<MultipartFile> files) {
        List<String> urlList = fileAppService.uploadFileList(files);
        return Response.ok(
                ResultCodeEnum.SUCCESS_LANG.getCode(),
                i18nMessageFacade.getMessage(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()),
                urlList
        );
    }

    /**
     * 文件状态检查接口（实现秒传与断点续传的核心）
     * <p>
     * 根据文件的 MD5 指纹检查文件的上传状态：
     * 1. <b>秒传</b>：如果文件已完整存在，直接返回 finished=true 和 fileUrl。
     * 2. <b>断点续传</b>：如果文件上传中断，返回 finished=false 和已上传的分片索引列表 (uploadedParts)。
     * 3. <b>新上传</b>：如果无记录，返回空状态，提示前端需要进行初始化。
     *
     * @param md5 文件的唯一 MD5 哈希值
     * @return Response&lt;FileCheckDTO&gt; 包含文件状态、URL 或已上传分片信息的 DTO
     */
    @GetMapping("/check")
    public Response<FileCheckDTO> checkFileByMd5(@RequestParam String md5) {
        FileCheckDTO fileCheckDTO = fileAppService.checkFileByMd5(md5);
        return Response.ok(
                ResultCodeEnum.SUCCESS_LANG.getCode(),
                i18nMessageFacade.getMessage(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()),
                fileCheckDTO
        );
    }

    /**
     * 初始化分片上传任务接口
     * <p>
     * 当 check 接口返回文件未完成时调用。
     * 该接口会向 MinIO 申请 UploadId，并在数据库生成上传任务记录。
     *
     * @param query 包含文件 MD5、文件名、文件大小、总分片数等初始化信息的查询对象
     * @return Response&lt;Void&gt; 成功初始化不返回具体数据，仅返回成功状态
     */
    @PostMapping("/init")
    public Response<Void> init(@RequestBody FileUploadQuery query) {
        fileAppService.initUpload(query);
        return Response.ok(
                ResultCodeEnum.SUCCESS_LANG.getCode(),
                i18nMessageFacade.getMessage(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()),
                null
        );
    }

    /**
     * 分片上传接口
     * <p>
     * 用于并发上传文件的各个分片。
     * 前端需根据 check 接口返回的 {@code uploadedParts} 计算出还需要上传哪些分片，
     * 然后循环调用此接口。
     *
     * @param md5        文件的 MD5 值，用于关联上传任务
     * @param chunkIndex 当前分片的索引（通常从 0 开始，后端会自动转换为 MinIO 需要的 1-based index）
     * @param file       当前分片的二进制数据流
     * @return Response&lt;Void&gt; 上传成功仅返回状态码
     * @throws Exception 上传过程中的网络或 IO 异常
     */
    @PostMapping("/upload/chunk")
    public Response<Void> uploadChunk(@RequestParam String md5,
                                      @RequestParam Integer chunkIndex,
                                      @RequestParam MultipartFile file) throws Exception {
        fileAppService.uploadChunk(md5, chunkIndex, file);
        return Response.ok(
                ResultCodeEnum.SUCCESS_LANG.getCode(),
                i18nMessageFacade.getMessage(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()),
                null
        );
    }

    /**
     * 合并分片接口
     * <p>
     * 当所有分片均上传成功后，由前端主动触发。
     * 后端会通知 MinIO 将所有分片合并为一个完整文件，并生成最终的文件记录。
     *
     * @param query 包含文件 MD5 和文件名（用于生成记录）的查询对象
     * @return Response&lt;String&gt; 合并成功后返回文件的最终访问 URL
     */
    @PostMapping("/merge")
    public Response<String> merge(@RequestBody FileUploadQuery query) {
        String url = fileAppService.mergeFile(query);
        return Response.ok(
                ResultCodeEnum.SUCCESS_LANG.getCode(),
                i18nMessageFacade.getMessage(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()),
                url
        );
    }

    /**
     * 取消上传任务接口
     * <p>
     * 用于中途终止上传。
     * 会清理 MinIO 端的临时分片数据（释放空间）以及本地数据库的任务记录（防止错误的断点续传）。
     *
     * @param md5 文件的 MD5 值
     * @return Response&lt;Void&gt; 取消成功仅返回状态码
     */
    @PostMapping("/cancel")
    public Response<Void> cancelUpload(@RequestParam String md5) {
        fileAppService.cancelUpload(md5);
        return Response.ok(
                ResultCodeEnum.SUCCESS_LANG.getCode(),
                i18nMessageFacade.getMessage(ResultCodeEnum.SUCCESS_LANG.getMessage(), LocaleContextHolder.getLocale()),
                null
        );
    }
}