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
 * 已完成上传文件记录表
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "file_record")
public class FileRecordPO {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文件的MD5哈希值，用于唯一标识文件内容，实现秒传功能
     */
    @TableField(value = "file_md5")
    private String fileMd5;

    /**
     * 原始文件名（包含扩展名）
     */
    @TableField(value = "file_name")
    private String fileName;

    /**
     * 文件大小（单位：字节）
     */
    @TableField(value = "file_size")
    private Long fileSize;

    /**
     * 文件的访问URL（MinIO网关地址或CDN地址）
     */
    @TableField(value = "file_url")
    private String fileUrl;

    /**
     * MinIO存储桶名称
     */
    @TableField(value = "bucket_name")
    private String bucketName;

    /**
     * MinIO对象存储路径（Key），例如：2023-10-01/md5/filename.ext
     */
    @TableField(value = "object_key")
    private String objectKey;

    /**
     * 记录创建时间（即文件上传完成时间）
     */
    @TableField(value = "create_time",fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}