package com.winter.cloud.auth.application.service.impl;


import cn.hutool.core.util.ObjectUtil;
import com.winter.cloud.auth.api.dto.response.DeptResponseDTO;
import com.winter.cloud.auth.application.assembler.AuthDeptAppAssembler;
import com.winter.cloud.auth.application.service.AuthDeptAppService;
import com.winter.cloud.auth.domain.model.entity.AuthDeptDO;
import com.winter.cloud.auth.domain.repository.AuthDeptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthDeptAppServiceImpl implements AuthDeptAppService {
    private final AuthDeptRepository authDeptRepository;
    private final AuthDeptAppAssembler authDeptAppAssembler;

    @Override
    public List<DeptResponseDTO> selectAllRecursionDept(String deptName, String status) {
        log.info("根据部门名和状态查询部门信息，deptName={}, status={}", deptName, status);
        List<AuthDeptDO> allDeptInfo = authDeptRepository.selectAllDept(deptName, status);
        // 从小到大排序
        List<AuthDeptDO> collect = allDeptInfo.stream().sorted((o1, o2) -> o1.getParentId().compareTo(o2.getParentId())).collect(Collectors.toList());
        Long parentId = 0L;
        if(ObjectUtil.isNotEmpty(collect)){
            parentId = collect.get(0).getParentId();
        }
        // 递归处理部门
        List<DeptResponseDTO> dtoList = authDeptAppAssembler.toDTOList(collect);
        return builderDeptTree(dtoList,parentId);
    }

    private List<DeptResponseDTO> builderDeptTree(List<DeptResponseDTO> deptDOList,Long parentId) {
        return deptDOList.stream()
                //将parentId为参数传递的部门留下来
                .filter(dept -> dept.getParentId().equals(parentId))
                //设置子菜单
                .map(dept -> dept.setChildren(getChildren(dept, deptDOList)))
                //收集返回
                .collect(Collectors.toList());
    }

    //获取传入参数的子部门
    private List<DeptResponseDTO> getChildren(DeptResponseDTO dept, List<DeptResponseDTO> menuVoList) {
        return menuVoList.stream().filter(m -> m.getParentId().equals(dept.getId()))
                .map(m -> m.setChildren(getChildren(m, menuVoList)))
                .collect(Collectors.toList());
    }
}