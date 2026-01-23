package com.winter.cloud.file.api.facade;

import com.winter.cloud.common.response.Response;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FileFacade {
    // 上传简单的单文件
    Response<String> uploadFile(MultipartFile file) throws IOException;
    // 上传批量文件
    Response<List<String>> uploadFileList(List<MultipartFile> files);

}
