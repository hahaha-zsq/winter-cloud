package com.winter.cloud.auth.application.service.impl;


import com.winter.cloud.auth.api.dto.command.UpsertRoleCommand;
import com.winter.cloud.auth.api.dto.query.RoleQuery;
import com.winter.cloud.auth.api.dto.response.RoleResponseDTO;
import com.winter.cloud.auth.application.assembler.AuthRoleAppAssembler;
import com.winter.cloud.auth.application.service.AuthRoleAppService;
import com.winter.cloud.auth.domain.model.entity.AuthRoleDO;
import com.winter.cloud.auth.domain.repository.AuthRoleRepository;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.exception.BusinessException;
import com.winter.cloud.common.response.PageAndOrderDTO;
import com.winter.cloud.common.response.PageDTO;
import com.zsq.i18n.template.WinterI18nTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthRoleAppServiceImpl implements AuthRoleAppService {

    private final AuthRoleRepository authRoleRepository;
    private final AuthRoleAppAssembler authRoleAppAssembler;
    private final WinterI18nTemplate winterI18nTemplate;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean roleSave(UpsertRoleCommand command) {
        log.info("保存角色信息，command={}", command);
        AuthRoleDO aDo = authRoleAppAssembler.toDO(command);
        return authRoleRepository.roleSave(aDo);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean roleUpdate(UpsertRoleCommand command) {
        log.info("更新角色信息，command={}", command);
        AuthRoleDO aDo = authRoleAppAssembler.toDO(command);
        return authRoleRepository.roleUpdate(aDo);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean roleDelete(List<Long> roleIds) {
        log.info("删除角色信息，roleIds={}", roleIds);
        return authRoleRepository.roleDelete(roleIds);
    }

    @Override
    public PageDTO<RoleResponseDTO> rolePage(RoleQuery roleQuery) {
        if (ObjectUtils.isEmpty(roleQuery) || ObjectUtils.isEmpty(roleQuery.getOrders())) {
            roleQuery.setOrders(Collections.emptyList());
        }
        List<PageAndOrderDTO.OrderDTO> normalizedOrders = normalizeAndValidateOrders(roleQuery.getOrders());

        roleQuery.setOrders(normalizedOrders);

        PageDTO<AuthRoleDO> doPage = authRoleRepository.rolePage(roleQuery);
        List<RoleResponseDTO> dtoList = authRoleAppAssembler.toDTOList(doPage.getRecords());
        return new PageDTO<>(dtoList, doPage.getTotal());
    }

    @Override
    public List<RoleResponseDTO> roleDynamicQueryList(RoleQuery roleQuery) {
        log.info("根据角色名和状态查询角色信息，roleName={}, status={}", roleQuery.getRoleName(), roleQuery.getStatus());
        List<AuthRoleDO> allRoleInfo = authRoleRepository.roleDynamicQueryList(roleQuery);
        return authRoleAppAssembler.toDTOList(allRoleInfo);
    }

    @Override
    public void assignMenuPermissions(Long roleId, List<Long> menuIds) {
        authRoleRepository.assignMenuPermissions(roleId, menuIds);
    }

    @Override
    public void roleExportExcel(HttpServletResponse response) {
        authRoleRepository.roleExportExcel(response);
    }

    @Override
    public void roleExportExcelTemplate(HttpServletResponse response) {
        authRoleRepository.roleExportExcelTemplate(response);
    }

    @Override
    public void roleImportExcel(HttpServletResponse response, MultipartFile file) throws IOException {
        authRoleRepository.roleImportExcel(response, file);

    }

    /**
     * 对排序参数进行【校验 + 标准化】的统一处理方法
     * <p>
     * 主要职责：
     * 1. 校验排序字段 field 是否在允许的白名单中（防 SQL 注入）
     * 2. 校验排序方式 order 是否合法（asc / desc / ascend / descend）
     * 3. 按 sequence 字段对排序条件进行排序（保证多字段排序顺序正确）
     * 4. 将排序方式统一标准化为数据库可识别的 "asc" / "desc"
     *
     * @param orders 前端传入的排序参数列表
     * @return 经过校验、排序、标准化后的排序参数列表
     */
    private List<PageAndOrderDTO.OrderDTO> normalizeAndValidateOrders(
            List<PageAndOrderDTO.OrderDTO> orders) {

        // 1️⃣ 如果前端未传排序参数，直接返回空列表
        // 由上层逻辑决定是否使用默认排序
        if (ObjectUtils.isEmpty(orders)) {
            return Collections.emptyList();
        }

        // 2️⃣ 允许排序的字段白名单（数据库真实字段名）
        // ⚠️ 这里是防 SQL 注入的关键点
        Set<String> allowedFields = Set.of(
                "role_sort",
                "status",
                "create_time"
        );

        // 3️⃣ 允许的排序方式（前端可能传多种写法）
        // 后续会统一转换为 asc / desc
        Set<String> allowedOrders = Set.of(
                "ascend",
                "asc",
                "descend",
                "desc"
        );

        return orders.stream()
                // 4️⃣ 校验阶段（不修改数据，只做合法性检查）
                .peek(order -> {
                    String field = order.getField();
                    String orderStr = order.getOrder();
                    // 4.1 校验排序字段是否合法
                    //  - 不能为空
                    //  - 必须在白名单中
                    if (ObjectUtils.isEmpty(field) || !allowedFields.contains(field)) {
                        throw new BusinessException(
                                ResultCodeEnum.FAIL_LANG.getCode(),
                                winterI18nTemplate.message(
                                        CommonConstants.I18nKey.SORT_KEY_ILLEGAL
                                )
                        );
                    }
                    // 4.2 校验排序方式是否合法
                    //  - 不能为空
                    //  - 忽略大小写后必须在允许集合中
                    if (ObjectUtils.isEmpty(orderStr)
                        || !allowedOrders.contains(orderStr.toLowerCase())) {
                        throw new BusinessException(
                                ResultCodeEnum.FAIL_LANG.getCode(),
                                winterI18nTemplate.message(
                                        CommonConstants.I18nKey.SORT_ORDER_ILLEGAL
                                )
                        );
                    }
                })

                // 5️⃣ 按 sequence 字段排序
                // 确保多字段排序时，顺序与前端约定一致
                .sorted(Comparator.comparing(PageAndOrderDTO.OrderDTO::getSequence))

                // 6️⃣ 对排序方式进行标准化处理（ascend → asc，descend → desc）
                .map(this::normalizeOrderValue)

                // 7️⃣ 收集为 List
                .collect(Collectors.toList());
    }

    /**
     * 对单个排序参数的 order 值进行标准化处理
     * <p>
     * 目标：
     * - 将前端可能传入的多种排序写法：
     * ascend / ASCEND / asc → asc
     * descend / DESCEND / desc → desc
     * - 返回一个新的 OrderDTO，避免直接修改原对象（更安全）
     *
     * @param dto 原始排序参数对象
     * @return 标准化后的排序参数对象
     */
    private PageAndOrderDTO.OrderDTO normalizeOrderValue(PageAndOrderDTO.OrderDTO dto) {

        String order = dto.getOrder();
        // （如果允许 order 为空，通常应在上游已拦截）
        if (!ObjectUtils.isEmpty(order)) {
            String lower = order.toLowerCase();
            // 将前端排序方式统一为数据库可用的写法
            if ("ascend".equals(lower) || "asc".equals(lower)) {
                order = "asc";
            } else if ("descend".equals(lower) || "desc".equals(lower)) {
                order = "desc";
            }
        }
        // 创建新的 OrderDTO 对象，避免修改原始参数对象
        // 如果 OrderDTO 是不可变对象（builder / withXxx），会更理想
        PageAndOrderDTO.OrderDTO normalized = new PageAndOrderDTO.OrderDTO();
        normalized.setField(dto.getField());
        normalized.setOrder(order);
        normalized.setSequence(dto.getSequence());

        return normalized;
    }
}