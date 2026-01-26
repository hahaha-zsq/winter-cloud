package com.winter.cloud.file.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分片上传任务临时表
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "multipart_upload_task")
public class MultipartUploadTaskPO {
    /**
     * 文件的MD5哈希值，作为主键，标识当前正在进行的上传任务
     */
    @TableId(value = "file_md5")
    private String fileMd5;

    /**
     * MinIO/S3 分片上传任务ID (UploadId)，用于后续的分片上传、合并或查询已上传分片
     */
    @TableField(value = "upload_id")
    private String uploadId;

    /**
     * 所属存储桶名称
     */
    @TableField(value = "bucket_name")
    private String bucketName;

    /**
     * 在MinIO中的目标存储路径（Key）
     */
    @TableField(value = "object_key")
    private String objectKey;

    /**
     * 文件被切分的总分片数（用于前端进度计算或校验）
     */
    @TableField(value = "total_chunks")
    private Integer totalChunks;

    /**
     * 最后更新时间，用于清理长时间未完成的过期任务
     */
    @TableField(value = "update_time",fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}