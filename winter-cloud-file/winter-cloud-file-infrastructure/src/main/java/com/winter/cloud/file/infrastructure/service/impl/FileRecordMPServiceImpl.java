package com.winter.cloud.file.infrastructure.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.winter.cloud.file.infrastructure.mapper.FileRecordMapper;
import com.winter.cloud.file.infrastructure.entity.FileRecordPO;
import com.winter.cloud.file.infrastructure.service.FileRecordMPService;
@Service
public class FileRecordMPServiceImpl extends ServiceImpl<FileRecordMapper, FileRecordPO> implements FileRecordMPService {

}
