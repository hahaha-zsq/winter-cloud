package com.winter.cloud.file.infrastructure.repository;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.file.domain.repository.FileRepository;
import com.zsq.winter.minio.service.AmazonS3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileRepositoryImpl implements FileRepository {
    private final AmazonS3Template amazonS3Template;
    @Value("${spring.application.name}")
    private String serviceName;
    @Value("${winter-aws.bucket}")
    private String bucketName;

    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        // 原始文件名
        String fileName = file.getOriginalFilename();
        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        // 文件后缀名
        String suffix = StringUtils.getFilenameExtension(fileName);
        // 文件名称
        String key = StrUtil.format("{}/{}/{}.{}", now.format(DateTimeFormatter.ofPattern(CommonConstants.DateTime.DATE_PATTERN)), serviceName, IdUtil.randomUUID(), suffix);
        // 上传文件到指定的bucket，不传递bucketName就会使用配置文件配置的默认的
        amazonS3Template.putObject(bucketName, key, file, null);
        return amazonS3Template.getGatewayUrl(key);
    }

    @Override
    public List<String> uploadFileList(List<MultipartFile> files) {
        List<String> urlList = new ArrayList<>(files.size());
        files.forEach(file -> {
            try {
                urlList.add(uploadFile(file));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return urlList;
    }
}
