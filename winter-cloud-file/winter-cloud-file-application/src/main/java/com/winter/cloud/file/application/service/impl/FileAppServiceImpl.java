package com.winter.cloud.file.application.service.impl;

import com.winter.cloud.file.application.service.FileAppService;
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

    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        return fileRepository.uploadFile(file);
    }

    @Override
    public List<String> uploadFileList(List<MultipartFile> files) {
        return fileRepository.uploadFileList(files);
    }
}
