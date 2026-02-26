package com.winter.cloud.auth.application.service;

import com.winter.cloud.auth.api.dto.command.UpsertMenuCommand;
import com.winter.cloud.auth.api.dto.query.MenuQuery;
import com.winter.cloud.auth.api.dto.response.MenuResponseDTO;

import javax.validation.constraints.NotNull;
import java.util.List;

public interface AuthMenuAppService {
    List<MenuResponseDTO> getMenu(Long userId);

    List<MenuResponseDTO> getDynamicRouting(@NotNull Long id);

    List<MenuResponseDTO> menuTree(MenuQuery menuQuery);

    boolean menuSave(UpsertMenuCommand command);

    List<Long> resourcesOwnedList(@NotNull Long roleId);
}