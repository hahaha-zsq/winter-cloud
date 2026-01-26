package com.winter.cloud.file.api.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class FileUploadQuery {
    private String md5;
    private String fileName;
    private Long fileSize;
    private Integer totalChunks;
}