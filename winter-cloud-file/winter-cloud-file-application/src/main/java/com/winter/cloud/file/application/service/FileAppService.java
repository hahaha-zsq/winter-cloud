package com.winter.cloud.file.application.service;

import com.winter.cloud.file.api.dto.query.FileUploadQuery;
import com.winter.cloud.file.api.dto.response.FileCheckDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


public interface FileAppService {

    // 上传简单的单文件
    String uploadFile(MultipartFile file) throws IOException;

    // 上传批量文件
    List<String> uploadFileList(List<MultipartFile> files);

    FileCheckDTO checkFileByMd5(String md5);

    void initUpload(FileUploadQuery query);

    void uploadChunk(String md5, Integer chunkIndex, MultipartFile file) throws Exception;

    String mergeFile(FileUploadQuery query);

    void cancelUpload(String md5);
}