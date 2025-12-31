package com.winter.cloud.auth.infrastructure.repository;

import cn.hutool.core.util.ObjectUtil;
import com.winter.cloud.auth.api.dto.response.MenuResponseDTO;
import com.winter.cloud.auth.domain.repository.AuthMenuRepository;
import com.winter.cloud.auth.infrastructure.mapper.AuthMenuMapper;
import com.winter.cloud.auth.infrastructure.service.IAuthMenuMpService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AuthMenuRepositoryImpl implements AuthMenuRepository {
    private final IAuthMenuMpService authMenuMpService;
    private final AuthMenuMapper authMenuMapper;



    @Override
    public List<MenuResponseDTO> selectMenuListByRoleIdList(List<Long> roleIdList, String status) {
        if(ObjectUtil.isNotEmpty(roleIdList)){
            return authMenuMapper.selectMenuListByRoleIdList(roleIdList,status);
        }
        return List.of();
    }

    @Override
    public List<MenuResponseDTO> getMenu(Long id) {
        if(ObjectUtil.isNotEmpty(id)){
            return authMenuMapper.getMenu(id);
        }
        return List.of();
    }
}
