package com.winter.cloud.auth.application.service.impl;


import com.winter.cloud.auth.api.dto.command.UpsertPostCommand;
import com.winter.cloud.auth.api.dto.query.PostQuery;
import com.winter.cloud.auth.api.dto.response.PostResponseDTO;
import com.winter.cloud.auth.application.assembler.AuthPostAppAssembler;
import com.winter.cloud.auth.application.service.AuthPostAppService;
import com.winter.cloud.auth.domain.model.entity.AuthPostDO;
import com.winter.cloud.auth.domain.repository.AuthPostRepository;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.exception.BusinessException;
import com.winter.cloud.common.response.PageAndOrderDTO;
import com.winter.cloud.common.response.PageDTO;
import com.zsq.i18n.template.WinterI18nTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 职位应用服务实现类
 * <p>
 * 负责处理职位相关的业务逻辑，包括职位的查询、分页、保存、更新、删除、导入导出等功能。
 *
 * @author winter
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthPostAppServiceImpl implements AuthPostAppService {

    private final AuthPostRepository authPostRepository;
    private final AuthPostAppAssembler authPostAppAssembler;
    private final WinterI18nTemplate winterI18nTemplate;


    /**
     * 根据职位名称和状态动态查询职位列表
     *
     * @param postQuery 查询条件，包含职位名称和状态
     * @return 职位信息列表
     */
    @Override
    public List<PostResponseDTO> postDynamicQueryList(PostQuery postQuery) {
        log.info("根据职位名和状态查询职位信息，postName={}, status={}", postQuery.getPostName(), postQuery.getStatus());
        List<AuthPostDO> allPostInfo = authPostRepository.postDynamicQueryList(postQuery);
        return authPostAppAssembler.toDTOList(allPostInfo);
    }

    /**
     * 分页查询职位列表
     *
     * @param postQuery 查询条件，包含分页参数和排序参数
     * @return 分页后的职位信息
     */
    @Override
    public PageDTO<PostResponseDTO> postPage(PostQuery postQuery) {
        if (ObjectUtils.isEmpty(postQuery) || ObjectUtils.isEmpty(postQuery.getOrders())) {
            postQuery.setOrders(Collections.emptyList());
        }
        List<PageAndOrderDTO.OrderDTO> normalizedOrders = normalizeAndValidateOrders(postQuery.getOrders());

        postQuery.setOrders(normalizedOrders);

        PageDTO<AuthPostDO> doPage = authPostRepository.postPage(postQuery);
        List<PostResponseDTO> dtoList = authPostAppAssembler.toDTOList(doPage.getRecords());
        return new PageDTO<>(dtoList, doPage.getTotal());
    }

    /**
     * 保存职位信息
     *
     * @param command 职位创建/更新命令对象
     * @return 是否保存成功
     */
    @Override
    public Boolean postSave(UpsertPostCommand command) {
        AuthPostDO aDo = authPostAppAssembler.toDO(command);
        return authPostRepository.postSave(aDo);
    }

    /**
     * 更新职位信息
     *
     * @param command 职位创建/更新命令对象
     * @return 是否更新成功
     */
    @Override
    public Boolean postUpdate(UpsertPostCommand command) {
        AuthPostDO aDo = authPostAppAssembler.toDO(command);
        return authPostRepository.postUpdate(aDo);
    }

    /**
     * 删除职位信息
     *
     * @param postIdList 职位ID列表
     * @return 是否删除成功
     */
    @Override
    public Boolean postDelete(List<Long> postIdList) {
        return authPostRepository.postDelete(postIdList);
    }

    /**
     * 导出职位Excel模板
     *
     * @param response HTTP响应对象，用于输出Excel模板
     */
    @Override
    public void postExportExcelTemplate(HttpServletResponse response) {
        authPostRepository.postExportExcelTemplate(response);
    }

    /**
     * 导入职位Excel数据
     *
     * @param response HTTP响应对象，用于输出导入结果
     * @param file 上传的Excel文件
     * @throws IOException 如果读取文件发生IO异常
     */
    @Override
    public void postImportExcel(HttpServletResponse response, MultipartFile file) throws IOException {
        authPostRepository.postImportExcel(response, file);
    }

    /**
     * 导出职位Excel数据
     *
     * @param response  HTTP响应对象，用于输出Excel文件
     * @param postQuery 查询条件，用于筛选导出的数据
     */
    @Override
    public void postExportExcel(HttpServletResponse response, PostQuery postQuery) {
        Boolean exportAll = postQuery.getExportAll();
        if (exportAll) {
            postQuery.setPageSize(1000000);
        }
        List<PostResponseDTO> records = this.postPage(postQuery).getRecords();
        List<AuthPostDO> doList = authPostAppAssembler.toDOList(records);
        authPostRepository.postExportExcel(response, doList);
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
                "status",
                "create_time",
                "order_num"
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