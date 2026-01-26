package com.winter.cloud.file.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskInfoDO {

    private boolean finished; // 是否秒传成功
    private String fileUrl;   // 如果秒传成功，返回URL
    private List<Integer> uploadedParts; // 如果未完成，返回已上传的分片号

}
