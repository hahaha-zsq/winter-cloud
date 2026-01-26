package com.winter.cloud.file.infrastructure.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.winter.cloud.file.infrastructure.mapper.MultipartUploadTaskMapper;
import com.winter.cloud.file.infrastructure.entity.MultipartUploadTaskPO;
import com.winter.cloud.file.infrastructure.service.MultipartUploadTaskMPService;
@Service
public class MultipartUploadTaskMPServiceImpl extends ServiceImpl<MultipartUploadTaskMapper, MultipartUploadTaskPO> implements MultipartUploadTaskMPService {

}
