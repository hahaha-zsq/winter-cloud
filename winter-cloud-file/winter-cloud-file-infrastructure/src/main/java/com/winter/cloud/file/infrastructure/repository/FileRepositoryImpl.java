package com.winter.cloud.file.infrastructure.repository;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PartListing;
import com.amazonaws.services.s3.model.PartSummary;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.file.api.dto.query.FileUploadQuery;
import com.winter.cloud.file.domain.model.entity.TaskInfoDO;
import com.winter.cloud.file.domain.repository.FileRepository;
import com.winter.cloud.file.infrastructure.entity.FileRecordPO;
import com.winter.cloud.file.infrastructure.entity.MultipartUploadTaskPO;
import com.winter.cloud.file.infrastructure.service.FileRecordMPService;
import com.winter.cloud.file.infrastructure.service.MultipartUploadTaskMPService;
import com.zsq.winter.minio.service.AmazonS3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件存储仓储实现类
 * <p>
 * 负责处理文件的具体存储逻辑，包括普通上传、分片上传前的状态检查（秒传/断点续传）。
 * 核心依赖 {@link AmazonS3Template} 与 AWS S3/MinIO 进行交互。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileRepositoryImpl implements FileRepository {

    // 核心 S3 操作模板类，封装了底层 AWS SDK 操作
    private final AmazonS3Template amazonS3Template;

    // 已完成文件的数据库记录服务（用于秒传）
    private final FileRecordMPService fileRecordMPService;

    // 分片上传任务的数据库记录服务（用于断点续传）
    private final MultipartUploadTaskMPService multipartUploadTaskMPService;

    // 从配置文件获取服务名称，用于构建文件存储路径
    @Value("${spring.application.name}")
    private String serviceName;

    // 从配置文件获取默认存储桶名称
    @Value("${winter-aws.bucket}")
    private String bucketName;

    /**
     * 单文件简单上传
     * <p>
     * 适用于小文件直接上传，不涉及分片逻辑。
     *
     * @param file 前端传递的 MultipartFile 文件对象
     * @return 上传成功后的文件访问网关 URL
     * @throws IOException 文件流读取异常
     */
    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        // 1. 获取原始文件名 (例如: "avatar.png")
        String fileName = file.getOriginalFilename();

        // 2. 获取当前时间，用于构建目录结构
        LocalDate now = LocalDate.now();

        // 3. 提取文件后缀名 (例如: "png")
        String suffix = StringUtils.getFilenameExtension(fileName);

        // 4. 构建唯一的文件存储路径 (Object Key)
        // 格式: 日期/服务名/UUID.后缀 -> 2023-10-01/user-service/550e8400-e29b....png
        // 使用 UUID 防止文件名冲突
        String key = StrUtil.format("{}/{}/{}.{}",
                now.format(DateTimeFormatter.ofPattern(CommonConstants.DateTime.DATE_PATTERN)),
                serviceName,
                IdUtil.randomUUID(),
                suffix);

        // 5. 调用 S3 模板进行上传
        // putObject 方法会将流传输到 MinIO
        //如果不传递 bucketName，SDK 内部会使用配置的默认 bucket
        amazonS3Template.putObject(bucketName, key, file, null);

        // 6. 获取并返回文件的完整访问 URL (Gateway URL)
        // 包含域名/IP、端口、BucketName 和文件名
        return amazonS3Template.getGatewayUrl(key);
    }

    /**
     * 批量上传文件
     *
     * @param files 文件列表
     * @return 上传成功的文件 URL 列表
     */
    @Override
    public List<String> uploadFileList(List<MultipartFile> files) {
        // 初始化结果列表，指定容量避免扩容
        List<String> urlList = new ArrayList<>(files.size());

        // 遍历上传每个文件
        files.forEach(file -> {
            try {
                // 复用单文件上传逻辑
                urlList.add(uploadFile(file));
            } catch (IOException e) {
                // 将受检异常包装为运行时异常抛出，中断批量操作
                throw new RuntimeException("批量上传失败: " + e.getMessage(), e);
            }
        });
        return urlList;
    }


    /**
     * 根据 MD5 检查文件上传状态
     * <p>
     * 核心方法：实现“秒传”和“断点续传”的智能判断。
     * 流程：
     * 1. 查 FileRecord 表 -> 有记录 -> 秒传成功 (Finished=true)。
     * 2. 查 MultipartUploadTask 表 -> 有记录 -> 断点续传 (Finished=false, 带回已传分片)。
     * 3. 均无记录 -> 全新上传 (Finished=false, 分片列表为空)。
     *
     * @param md5 文件的唯一数字摘要 (MD5)
     * @return TaskInfoDO 包含上传状态、文件 URL (若已完成) 或 已上传分片索引列表 (若未完成)
     */
    @Override
    public TaskInfoDO checkFileByMd5(String md5) {
        TaskInfoDO result = new TaskInfoDO();

        // =================================================================================
        // 第一步：检查是否“已完成”（秒传判断）
        // =================================================================================

        // 查询 file_record 表，看是否存在该 MD5 的记录
        FileRecordPO record = fileRecordMPService.getOne(
                new LambdaQueryWrapper<FileRecordPO>().eq(FileRecordPO::getFileMd5, md5)
        );

        // 如果记录存在 (ObjectUtil.isNotEmpty)，说明之前已经完整上传过
        if (ObjectUtil.isNotEmpty(record)) {
            // 设置状态为已完成
            result.setFinished(true);
            // 直接返回之前生成的 URL，前端直接显示，无需再次上传文件流
            result.setFileUrl(record.getFileUrl());
            // 返回结果，流程结束（实现秒传）
            return result;
        }

        // =================================================================================
        // 第二步：检查是否“部分上传”（断点续传判断）
        // =================================================================================

        // 如果上面没返回，说明文件要么是全新的，要么是传了一半断了
        // 查询 multipart_upload_task 表，获取挂起的任务信息
        MultipartUploadTaskPO task = multipartUploadTaskMPService.getById(md5);

        // 如果任务存在，说明之前初始化过上传，但未完成合并
        if (ObjectUtil.isNotEmpty(task)) {
            try {
                // 【关键逻辑】不能仅依赖本地数据库的状态，必须去 MinIO 服务端确认实际收到的分片
                // 调用 amazonS3Template.listParts 向 MinIO 发起请求
                // 参数: bucketName, objectKey, uploadId (MinIO 生成的唯一上传ID)
                PartListing partListing = amazonS3Template.listParts(
                        task.getBucketName(),
                        task.getObjectKey(),
                        task.getUploadId()
                );

                // 从 MinIO 返回的结果中提取所有 PartSummary (分片摘要)
                // 获取每个分片的 PartNumber (分片编号，通常从1开始)
                List<Integer> uploadedParts = partListing.getParts().stream()
                        .map(PartSummary::getPartNumber)
                        .collect(Collectors.toList());

                // 将 MinIO 确认已存在的分片列表返回给前端
                // 前端根据此列表，计算差集，只上传剩下的分片
                result.setUploadedParts(uploadedParts);

            } catch (Exception e) {
                // 异常处理：通常发生于 MinIO 中的 uploadId 已过期或被清理，但本地数据库还有残留记录
                log.warn("检查分片状态失败，MinIO中任务可能已失效，清理本地脏数据。MD5: {}", md5, e);

                // 清理本地无效的任务记录，强制前端重新发起初始化流程
                multipartUploadTaskMPService.removeById(md5);
            }
        }

        // 只要没进入“秒传”逻辑，Finished 都是 false
        // 如果是全新文件，uploadedParts 为 null 或空，前端会发起 init 接口
        // 如果是断点续传，uploadedParts 包含已传编号
        result.setFinished(false);
        return result;
    }

    /**
     * 初始化分片上传任务
     * <p>
     * 该方法用于在开始分片上传前进行环境准备。主要完成以下工作：
     * 1. 幂等性检查：防止重复初始化。
     * 2. 路径规划：生成文件在 MinIO 中的存储路径。
     * 3. 申请资源：向 MinIO 申请全局唯一的 UploadId。
     * 4. 任务持久化：在数据库记录任务信息，为断点续传做准备。
     *
     * @param query 上传请求参数对象，包含文件MD5、原始文件名、总分片数等关键信息
     */
    @Transactional(rollbackFor = Exception.class) // 开启事务，确保 MinIO 初始化成功且数据库保存成功，保证数据一致性
    @Override
    public void initUpload(FileUploadQuery query) {
        // =================================================================================
        // 1. 幂等性检查 (Idempotency Check)
        // =================================================================================
        // 根据文件的 MD5 查询数据库中是否已存在“正在进行中”的任务
        // 如果任务已存在 (ObjectUtil.isNotEmpty)，说明此前已经初始化过但尚未完成（或正在上传中）
        // 直接返回，保留原有的 UploadId，避免重复申请导致旧的任务 ID 失效或产生垃圾数据
        if (ObjectUtil.isNotEmpty(multipartUploadTaskMPService.getById(query.getMd5()))) {
            return;
        }

        // =================================================================================
        // 2. 构建存储路径 (ObjectKey Generation)
        // =================================================================================
        // 获取文件后缀名 (例如: "mp4", "jpg", "zip")，StringUtils 是 Spring 框架提供的工具类
        String suffix = StringUtils.getFilenameExtension(query.getFileName());

        // 规划文件在 MinIO 中的存储路径 (ObjectKey)
        // 格式策略: yyyy-MM-dd / 服务名 / 文件MD5 / 随机UUID.后缀
        // 示例: 2023-10-27/user-service/a1b2c3d4.../550e8400-e29b....mp4
        // - 日期目录: 避免单目录文件过多
        // - MD5目录: 方便根据指纹排查文件
        // - UUID文件名: 彻底避免同名文件覆盖冲突
        String objectKey = StrUtil.format("{}/{}/{}/{}.{}",
                LocalDate.now().format(DateTimeFormatter.ofPattern(CommonConstants.DateTime.DATE_PATTERN)),
                serviceName,
                query.getMd5(),
                IdUtil.randomUUID(),
                suffix);

        // =================================================================================
        // 3. 向 MinIO 申请 UploadId (Initiate Multipart Upload)
        // =================================================================================
        // 自动探测文件的 MIME 类型 (Content-Type)
        // 如果探测失败，默认使用 "application/octet-stream" (二进制流)，确保文件能被正确识别
        String contentType = MediaTypeFactory.getMediaType(query.getFileName())
                .orElse(MediaType.APPLICATION_OCTET_STREAM).toString();

        // 调用 AmazonS3Template 的 initiateMultipartUpload 方法
        // 这一步会向 S3 服务端发送请求，声明"我要开始传一个大文件了"，S3 会返回一个 UploadId
        InitiateMultipartUploadResult initResult = amazonS3Template.initiateMultipartUpload(bucketName, objectKey, contentType);

        // 获取初始化后的上传 ID，这个 ID 是后续所有分片上传和合并文件的唯一凭证
        String uploadId = initResult.getUploadId();

        // =================================================================================
        // 4. 持久化任务信息 (Persist Task Info)
        // =================================================================================
        // 创建分片上传任务实体对象 (PO)
        MultipartUploadTaskPO task = new MultipartUploadTaskPO();

        task.setFileMd5(query.getMd5());       // 设置主键：文件 MD5
        task.setUploadId(uploadId);            // 关键：保存 MinIO 返回的 UploadId
        task.setBucketName(bucketName);        // 保存存储桶名称，确保合并时能找到
        task.setObjectKey(objectKey);          // 保存生成的存储路径
        task.setTotalChunks(query.getTotalChunks()); // 保存总分片数，用于后续进度计算或完整性校验

        // 将任务保存到 MySQL 数据库
        // 这样当前端进行断点续传检查 (checkFileByMd5) 时，就能通过 MD5 找回这个 UploadId
        multipartUploadTaskMPService.save(task);
    }

    /**
     * 上传单个文件分片
     * <p>
     * 该方法负责将前端切分好的文件块上传到 MinIO。
     * 核心逻辑：
     * 1. 校验任务有效性：确保该分片属于一个正在进行的合法上传任务。
     * 2. 索引转换：适配前端分片索引与 S3 分片编号的差异。
     * 3. 执行上传：调用 SDK 将二进制流写入 MinIO。
     *
     * @param md5        文件的 MD5 唯一标识，用于关联分片上传任务
     * @param chunkIndex 当前分片的索引（前端通常从 0 开始）
     * @param file       当前分片的文件内容（MultipartFile）
     * @throws Exception 上传过程中可能出现的 IO 异常或 S3 服务端异常
     */
    @Override
    public void uploadChunk(String md5, Integer chunkIndex, MultipartFile file) throws Exception {
        // =================================================================================
        // 1. 校验上传任务状态 (Validate Task)
        // =================================================================================
        // 根据文件 MD5 从数据库查询当前的分片上传任务信息
        // 这一步是为了确保上传的分片是属于一个有效的、已初始化的任务
        MultipartUploadTaskPO multipartUploadTaskPO = multipartUploadTaskMPService.getById(md5);

        // 如果查询结果为空，说明任务可能已经被合并、取消，或者从未初始化过
        if (ObjectUtil.isEmpty(multipartUploadTaskPO)) {
            throw new RuntimeException("分片上传任务不存在或已失效，请重新初始化上传");
        }

        // =================================================================================
        // 2. 转换分片编号 (Convert Part Number)
        // =================================================================================
        // 前端切片插件（如 WebUploader, uploader.js）通常使用从 0 开始的数组索引
        // 而 AWS S3/MinIO 的分片编号 (PartNumber) 规范要求必须从 1 开始
        // 因此这里需要进行 +1 操作，否则 S3 服务端会报错
        int partNumber = chunkIndex + 1;

        // =================================================================================
        // 3. 执行分片上传 (Execute Upload)
        // =================================================================================
        // 调用 AmazonS3Template 的 uploadPart 方法将数据流上传到 MinIO
        // 参数说明：
        // - bucketName: 存储桶名称
        // - uploadId:   初始化时生成的唯一任务ID (从数据库获取)
        // - objectKey:  文件在 MinIO 中的完整路径 (从数据库获取)
        // - partNumber: 转换后的分片编号
        // - file:       分片文件流，SDK 内部会自动计算 MD5 进行传输校验
        amazonS3Template.uploadPart(
                bucketName,
                multipartUploadTaskPO.getUploadId(),
                multipartUploadTaskPO.getObjectKey(),
                partNumber,
                file
        );
    }

    /**
     * 合并分片文件
     * <p>
     * 当所有分片上传完成后，调用此方法通知 MinIO 进行合并，并生成最终的文件记录。
     * 核心逻辑：
     * 1. 任务校验：确保任务存在。
     * 2. 执行合并：调用 S3 接口合并分片。
     * 3. 数据持久化：保存文件记录到 file_record 表。
     * 4. 清理资源：删除 multipart_upload_task 表中的临时任务。
     *
     * @param query 包含文件 MD5 和文件名等合并请求信息的查询对象
     * @return 合并成功后的文件访问 URL
     */
    @Transactional(rollbackFor = Exception.class) // 开启事务，确保数据库操作（保存记录、删除任务）与 MinIO 操作的一致性
    @Override
    public String mergeFile(FileUploadQuery query) {
        // =================================================================================
        // 1. 获取任务信息 (Retrieve Task)
        // =================================================================================
        // 根据 MD5 获取挂起的分片上传任务
        MultipartUploadTaskPO multipartUploadTaskPO = multipartUploadTaskMPService.getById(query.getMd5());

        // 双重检查：防止重复调用合并，或任务已被清理
        if (ObjectUtil.isEmpty(multipartUploadTaskPO)) {
            throw new RuntimeException("合并失败：分片任务不存在或已失效");
        }

        // =================================================================================
        // 2. 通知 MinIO 合并分片 (Complete Multipart Upload)
        // =================================================================================
        // 调用 AmazonS3Template 的 completeMultipartUpload 方法
        // 【重要特性】：
        // 该 Starter 封装的无参 completeMultipartUpload 方法内部会自动调用 listParts 接口，
        // 从 MinIO 服务端获取该 UploadId 下所有已上传的分片信息 (ETag 和 PartNumber)，
        // 然后组装成 CompleteMultipartUploadRequest 发送给 MinIO。
        // 这大大简化了前端逻辑，前端无需在合并请求中携带庞大的 ETag 列表。
        amazonS3Template.completeMultipartUpload(
                multipartUploadTaskPO.getBucketName(),
                multipartUploadTaskPO.getObjectKey(),
                multipartUploadTaskPO.getUploadId()
        );

        // =================================================================================
        // 3. 生成访问链接 (Generate URL)
        // =================================================================================
        // 获取文件的网关访问 URL (例如：http://minio.example.com/bucket/path/to/file.ext)
        String fileUrl = amazonS3Template.getGatewayUrl(
                multipartUploadTaskPO.getBucketName(),
                multipartUploadTaskPO.getObjectKey()
        );

        // =================================================================================
        // 4. 持久化文件记录 (Save File Record)
        // =================================================================================
        // 创建文件记录对象，用于后续的“秒传”检查和业务查询
        FileRecordPO record = new FileRecordPO();
        record.setFileMd5(query.getMd5());           // 文件指纹
        record.setFileName(query.getFileName());     // 原始文件名
        record.setFileUrl(fileUrl);                  // 访问地址
        record.setBucketName(multipartUploadTaskPO.getBucketName()); // 存储桶
        record.setObjectKey(multipartUploadTaskPO.getObjectKey());   // 存储路径

        // 保存到 file_record 表
        fileRecordMPService.save(record);

        // =================================================================================
        // 5. 清理临时任务 (Clean up Task)
        // =================================================================================
        // 文件已合并且记录已保存，分片任务表中的临时记录不再需要
        // 删除该记录，防止垃圾数据堆积，同时防止该任务被再次操作
        multipartUploadTaskMPService.removeById(query.getMd5());

        // 返回最终的文件访问路径
        return fileUrl;
    }

    /**
     * 取消分片上传
     * <p>
     * 该方法用于中途终止上传任务，主要完成资源清理工作：
     * 1. 终止 S3/MinIO 端的上传任务，释放服务端因存储分片占用的临时空间。
     * 2. 清除本地数据库中的任务记录，重置上传状态。
     *
     * @param md5 文件的 MD5 唯一标识
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancelUpload(String md5) {
        // 1. 查询当前正在进行的任务
        MultipartUploadTaskPO task = multipartUploadTaskMPService.getById(md5);

        // 如果任务不存在，可能是已经完成、已经取消，或者从未开始
        // 这里的处理策略是直接返回，视为“取消成功”（幂等性设计）
        if (ObjectUtil.isEmpty(task)) {
            log.warn("尝试取消不存在的任务，MD5: {}", md5);
            return;
        }

        try {
            // 2. 通知 MinIO 终止上传
            // 调用 starter 提供的 abortMultipartUpload 方法
            // 这会告诉 MinIO："这个 UploadId 不要了，把已经收到的 1, 2, 5... 号分片都删掉吧"
            amazonS3Template.abortMultipartUpload(
                    task.getBucketName(),
                    task.getObjectKey(),
                    task.getUploadId()
            );
            log.info("MinIO 分片任务已终止. UploadId: {}", task.getUploadId());
        } catch (Exception e) {
            // 即使 MinIO 端报错（例如 uploadId 早就失效了），我们也应该继续清理本地数据库
            // 记录日志即可，不要阻断流程
            log.error("MinIO 终止任务失败 (可能是任务已过期)，继续清理本地记录. MD5: {}", md5, e);
        }

        // 3. 清理本地数据库记录
        // 删除任务后，下次该文件再上传时，checkFileByMd5 将查询不到任务，从而触发全新的 /init 流程
        multipartUploadTaskMPService.removeById(md5);
    }
}
