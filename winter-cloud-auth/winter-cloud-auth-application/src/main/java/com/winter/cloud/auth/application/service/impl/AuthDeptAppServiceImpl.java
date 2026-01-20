package com.winter.cloud.auth.application.service.impl;


import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.winter.cloud.auth.api.dto.response.DeptResponseDTO;
import com.winter.cloud.auth.application.assembler.AuthDeptAppAssembler;
import com.winter.cloud.auth.application.service.AuthDeptAppService;
import com.winter.cloud.auth.domain.model.entity.AuthDeptDO;
import com.winter.cloud.auth.domain.repository.AuthDeptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthDeptAppServiceImpl implements AuthDeptAppService {
    private final AuthDeptRepository authDeptRepository;
    private final AuthDeptAppAssembler authDeptAppAssembler;

    @Override
    public List<DeptResponseDTO> selectAllRecursionDept(String deptName, String status) {
        // 1. 获取全量数据 (不传 name 过滤，只传 status，保证能构建完整树结构)
        List<AuthDeptDO> allDeptDOs = authDeptRepository.selectAllDept(null, status);
        // 转换 DO 为 DTO
        List<DeptResponseDTO> allDTOs = authDeptAppAssembler.toDTOList(allDeptDOs);

        // 2. 核心优化：利用 Map 按 parentId 分组
        // Key: parentId, Value: 该父节点下的所有子节点列表
        Map<Long, List<DeptResponseDTO>> parentToChildrenMap = allDTOs.stream()
                .collect(Collectors.groupingBy(DeptResponseDTO::getParentId));

        // 3. 遍历所有节点，自动挂载子节点 (利用对象引用特性)
        allDTOs.forEach(dept -> {
            List<DeptResponseDTO> children = parentToChildrenMap.get(dept.getId());
            if (children != null) {
                // 可选：对子节点排序 (如果有 orderNum 字段)
                children.sort(Comparator.comparingInt(DeptResponseDTO::getOrderNum));
                dept.setChildren(children);
            }
        });
        // 4. 根据查询条件返回结果
        if (StrUtil.isNotEmpty(deptName)) {
            // 如果有搜索关键词，返回名字匹配的节点（它们的 children 已经在第3步被挂载好了）
            return allDTOs.stream()
                    .filter(dept -> dept.getDeptName().contains(deptName))
                    .collect(Collectors.toList());
        } else {
            // 如果没有搜索条件，返回顶级节点 (parentId 为 0 或 null 的)
            // 注意：这里直接从 Map 取 parentId=0 的列表即可，效率最高
            return parentToChildrenMap.getOrDefault(0L, new ArrayList<>());
        }
    }
}