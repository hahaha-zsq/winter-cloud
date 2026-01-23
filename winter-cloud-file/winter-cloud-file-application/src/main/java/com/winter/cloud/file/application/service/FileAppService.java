package com.winter.cloud.file.application.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


public interface FileAppService {

    // 上传简单的单文件
    String uploadFile(MultipartFile file) throws IOException;

    // 上传批量文件
    List<String> uploadFileList(List<MultipartFile> files);
}