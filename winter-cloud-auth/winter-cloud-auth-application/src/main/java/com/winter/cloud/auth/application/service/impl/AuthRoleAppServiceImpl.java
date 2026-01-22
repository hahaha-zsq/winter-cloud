package com.winter.cloud.auth.application.service.impl;


import com.winter.cloud.auth.api.dto.command.RoleCommand;
import com.winter.cloud.auth.api.dto.query.RoleQuery;
import com.winter.cloud.auth.api.dto.response.RoleResponseDTO;
import com.winter.cloud.auth.application.assembler.AuthRoleAppAssembler;
import com.winter.cloud.auth.application.service.AuthRoleAppService;
import com.winter.cloud.auth.domain.model.entity.AuthRoleDO;
import com.winter.cloud.auth.domain.repository.AuthRoleRepository;
import com.winter.cloud.common.response.PageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthRoleAppServiceImpl implements AuthRoleAppService {

    private final AuthRoleRepository authRoleRepository;
    private final AuthRoleAppAssembler authRoleAppAssembler;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean saveRole(RoleCommand command) {
        log.info("保存角色信息，command={}", command);
        AuthRoleDO aDo = authRoleAppAssembler.toDO(command);
        return authRoleRepository.saveRole(aDo);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateRole(RoleCommand command) {
        log.info("更新角色信息，command={}", command);
        AuthRoleDO aDo = authRoleAppAssembler.toDO(command);
        return authRoleRepository.updateRole(aDo);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean deleteRole(List<Long> roleIds) {
        log.info("删除角色信息，roleIds={}", roleIds);
        return authRoleRepository.deleteRole(roleIds);
    }

    @Override
    public PageDTO<RoleResponseDTO> rolePage(RoleQuery roleQuery) {
        log.info("分页查询角色信息，roleQuery={}", roleQuery);
        PageDTO<AuthRoleDO> authRoleDOPageDO = authRoleRepository.rolePage(roleQuery);
        //        DO->DTO
        List<RoleResponseDTO> dtoList = authRoleAppAssembler.toDTOList(authRoleDOPageDO.getRecords());
        return new PageDTO<>(dtoList, authRoleDOPageDO.getTotal());
    }

    @Override
    public List<RoleResponseDTO> getAllRoleInfo(String roleName, String status) {
        log.info("根据角色名和状态查询角色信息，roleName={}, status={}", roleName, status);
        List<AuthRoleDO> allRoleInfo = authRoleRepository.getAllRoleInfo(roleName, status);
        return authRoleAppAssembler.toDTOList(allRoleInfo);
    }
}