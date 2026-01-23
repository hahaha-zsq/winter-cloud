package com.winter.cloud.file.domain.repository;


import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 文件仓储接口 (面向领域)
 */
public interface FileRepository {
    // 上传简单的单文件
    String uploadFile(MultipartFile file) throws IOException;

    // 上传批量文件
    List<String> uploadFileList(List<MultipartFile> files);
}