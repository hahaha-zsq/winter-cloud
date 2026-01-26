package com.winter.cloud.file.application.service.impl;

import com.winter.cloud.file.api.dto.query.FileUploadQuery;
import com.winter.cloud.file.api.dto.response.FileCheckDTO;
import com.winter.cloud.file.application.assembler.FileAppAssembler;
import com.winter.cloud.file.application.service.FileAppService;
import com.winter.cloud.file.domain.model.entity.TaskInfoDO;
import com.winter.cloud.file.domain.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileAppServiceImpl implements FileAppService {
    private final FileRepository fileRepository;
    private final FileAppAssembler fileAppAssembler;

    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        return fileRepository.uploadFile(file);
    }

    @Override
    public List<String> uploadFileList(List<MultipartFile> files) {
        return fileRepository.uploadFileList(files);
    }

    @Override
    public FileCheckDTO checkFileByMd5(String md5) {
        TaskInfoDO taskInfoDO = fileRepository.checkFileByMd5(md5);
        return fileAppAssembler.toFileCheckDTO(taskInfoDO);
    }

    @Override
    public void initUpload(FileUploadQuery query) {
        fileRepository.initUpload(query);
    }

    @Override
    public void uploadChunk(String md5, Integer chunkIndex, MultipartFile file) throws Exception {
        fileRepository.uploadChunk(md5, chunkIndex, file);
    }

    @Override
    public String mergeFile(FileUploadQuery query) {
        return fileRepository.mergeFile(query);
    }

    @Override
    public void cancelUpload(String md5) {
        fileRepository.cancelUpload(md5);
    }
}
