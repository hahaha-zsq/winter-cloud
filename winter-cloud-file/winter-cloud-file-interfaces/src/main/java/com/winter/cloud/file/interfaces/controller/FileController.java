package com.winter.cloud.file.interfaces.controller;

import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.response.Response;
import com.winter.cloud.file.api.facade.FileFacade;
import com.winter.cloud.file.application.service.FileAppService;
import com.zsq.i18n.template.WinterI18nTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 文件控制
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/file")
public class FileController implements FileFacade {
    private final FileAppService fileAppService;
    private final WinterI18nTemplate winterI18nTemplate;

    /**
     * 直接上传（适合小文件）
     * @param file  文件
     */
    @PostMapping("/uploadFile")
    @Override
    public Response<String> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        String url = fileAppService.uploadFile(file);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(),winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()),url);
    }
    /**
     * 批量直接上传（适合小文件）
     * @param files  文件列表
     */
    @PostMapping("/uploadFileList")
    @Override
    public Response<List<String>> uploadFileList(@RequestParam("files")List<MultipartFile> files) {
        List<String> urlList = fileAppService.uploadFileList(files);
        return Response.ok(ResultCodeEnum.SUCCESS_LANG.getCode(),winterI18nTemplate.message(ResultCodeEnum.SUCCESS_LANG.getMessage()),urlList);
    }
}
