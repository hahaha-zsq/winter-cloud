package com.winter.cloud.auth.application.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winter.cloud.auth.api.dto.command.UserLoginCommand;
import com.winter.cloud.auth.api.dto.command.UserRegisterCommand;
import com.winter.cloud.auth.api.dto.query.PostQuery;
import com.winter.cloud.auth.api.dto.query.UserQuery;
import com.winter.cloud.auth.api.dto.response.*;
import com.winter.cloud.auth.application.assembler.AuthDeptAppAssembler;
import com.winter.cloud.auth.application.assembler.AuthPostAppAssembler;
import com.winter.cloud.auth.application.assembler.AuthRoleAppAssembler;
import com.winter.cloud.auth.application.assembler.AuthUserAppAssembler;
import com.winter.cloud.auth.application.service.AuthMenuAppService;
import com.winter.cloud.auth.application.service.AuthUserAppService;
import com.winter.cloud.auth.domain.model.entity.AuthDeptDO;
import com.winter.cloud.auth.domain.model.entity.AuthPostDO;
import com.winter.cloud.auth.domain.model.entity.AuthRoleDO;
import com.winter.cloud.auth.domain.model.entity.AuthUserDO;
import com.winter.cloud.auth.domain.repository.*;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.enums.StatusEnum;
import com.winter.cloud.common.exception.BusinessException;
import com.winter.cloud.common.response.PageAndOrderDTO;
import com.winter.cloud.common.response.PageDTO;
import com.winter.cloud.common.util.JwtUtil;
import com.zsq.winter.encrypt.util.CryptoUtil;
import com.zsq.winter.redis.ddc.service.WinterRedisTemplate; // 使用你的 Redis 工具
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.winter.cloud.common.enums.ResultCodeEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthUserAppServiceImpl implements AuthUserAppService {

    private final AuthUserRepository authUserRepository;
    private final AuthDeptRepository authDeptRepository;
    private final AuthPostRepository authPostRepository;
    private final AuthMenuAppService authMenuAppService;
    private final AuthRoleRepository authRoleRepository;
    private final AuthMenuRepository authMenuRepository;
    private final AuthUserAppAssembler authUserAppAssembler;
    private final WinterRedisTemplate winterRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AuthRoleAppAssembler authRoleAppAssembler;
    private final AuthDeptAppAssembler authDeptAppAssembler;
    private final AuthPostAppAssembler authPostAppAssembler;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean register(UserRegisterCommand command) {
        // 1. 校验是否存在重复的用户信息
        if (authUserRepository.hasDuplicateUser(command)) {
            throw new BusinessException(DUPLICATE_KEY.getCode(), "用户名/邮箱/电话已存在！");
        }
        // 2. 创建领域对象
        AuthUserDO authUserDO = authUserAppAssembler.toDO(command);
        // 3. 密码加密
        String encryptedPwd = CryptoUtil.winterMd5Hex16(authUserDO.getPassword());
        authUserDO.setPassword(encryptedPwd);

        authUserDO.setStatus(StatusEnum.ENABLE.getCode()); // 正常
        authUserDO.setDelFlag("0"); // 未删除

        // 4. 保存
        return authUserRepository.save(authUserDO);
    }

    @Override
    public LoginResponseDTO login(UserLoginCommand command) throws JsonProcessingException {
        AuthUserDO authUserDO = authUserRepository.findByEmail(command.getEmail());
        // 查询邮箱是否存在，不存在抛出异常，存在进行密码校验
        if (ObjectUtils.isEmpty(authUserDO)) {
            throw new BusinessException(NOT_FOUND);
        }
        // 密码校验
        if (!authUserDO.verifyPassword(command.getPassword(), authUserDO.getPassword())) {
            throw new BusinessException(LOGIN_FAILED);
        }
        // 账号状态校验
        if (!StatusEnum.ENABLE.getCode().equals(authUserDO.getStatus())) {
            throw new BusinessException(DISABLED.getCode(), "用户已停用");
        }
        // 生成token
        HashMap<String, Object> claim = new HashMap<>();
        claim.put(CommonConstants.Claim.NAME, authUserDO.getUserName());
        String token = JwtUtil.generateToken(String.valueOf(authUserDO.getId()), claim, CommonConstants.Redis.EXPIRATION_TIME);
        ValidateTokenDTO validateTokenDTO = generateUserInfo(authUserDO.getId(), authUserDO.getUserName());
        // 序列化
        String value = objectMapper.writeValueAsString(validateTokenDTO);
        // 缓存用户信息可以设置过期时间也可以不设置，网关先校验token有没有过期，没过期才会去用用户id去缓存查找
        winterRedisTemplate.set(CommonConstants.buildUserCacheKey(authUserDO.getId().toString()), value, CommonConstants.Redis.EXPIRATION_TIME, TimeUnit.MICROSECONDS);
        // 登录成功返回用户有哪些菜单、权限配置（递归父子级别）
        List<MenuResponseDTO> menu = authMenuAppService.getMenu(authUserDO.getId());
        List<String> permissions = validateTokenDTO.getPermissions();
        MenuAndButtonResponseDTO menuAndButtonResponseDTO = MenuAndButtonResponseDTO.builder()
                .menuList(menu)
                .buttonList(permissions)
                .build();

        LoginResponseDTO responseDTO = authUserAppAssembler.toResponseDTO(authUserDO);
        responseDTO.setToken(token);
        responseDTO.setMenuAndButton(menuAndButtonResponseDTO);
        return responseDTO;

    }


    @Override
    public ValidateTokenDTO generateUserInfo(Long userID, String userName) {
        // 数据库获取用户的角色和权限信息
        // 获取角色正常的信息
        List<AuthRoleDO> roleResponseDOList = authRoleRepository.selectRoleListByUserId(userID, StatusEnum.ENABLE.getCode());
        // 获取角色正常的权限标识
        List<String> roleKeyList = roleResponseDOList.stream().map(AuthRoleDO::getRoleKey).filter(ObjectUtil::isNotEmpty).distinct().collect(Collectors.toList());
        // 获取角色正常的角色id
        List<Long> roleIdList = roleResponseDOList.stream().map(AuthRoleDO::getId).filter(ObjectUtil::isNotEmpty).distinct().collect(Collectors.toList());
        // 根据角色id查询状态正常的权限并去重复
        List<MenuResponseDTO> menuResponseDTOList = authMenuRepository.selectMenuListByRoleIdList(roleIdList, StatusEnum.ENABLE.getCode());
        // 获取权限标识并去重复
        List<String> permissionsList = menuResponseDTOList.stream().map(MenuResponseDTO::getPerms).filter(ObjectUtil::isNotEmpty).distinct().collect(Collectors.toList());

        return ValidateTokenDTO.builder()
                .valid(true)
                .userId(userID)
                .userName(userName)
                .roles(roleKeyList)
                .permissions(permissionsList).build();
    }

    @Override
    public PageDTO<UserResponseDTO> userPage(UserQuery userQuery) {
        List<PageAndOrderDTO.OrderDTO> orderDTOList = userQuery.getOrders();
        List<String> allowSortColumnList = List.of("sex", "status", "create_time");
        List<String> allowSortValue = List.of("ascend", "asc", "descend", "desc", "ASCEND", "ASC", "DESCEND", "DESC");
        // 判断排序字段是否在允许的字段列表中，只要有一个不在，就抛出异常
        orderDTOList.forEach(orderDTO -> {
            if (!allowSortColumnList.contains(orderDTO.getField())) {
                throw new BusinessException(ResultCodeEnum.FAIL_LANG.getCode(), "非法的排序字段！");
            }
            if (!allowSortValue.contains(orderDTO.getOrder())) {
                throw new BusinessException(ResultCodeEnum.FAIL_LANG.getCode(), "非法的排序方式！");
            }
        });
        // 对排序字段进行排序
        List<PageAndOrderDTO.OrderDTO> collect = orderDTOList.stream().sorted((o1, o2) -> o1.getSequence().compareTo(o2.getSequence()))
                .map(dto -> {
                    String newOrder = dto.getOrder();
                    if (newOrder != null) {
                        String lower = newOrder.toLowerCase();
                        if ("ascend".equals(lower)) {
                            newOrder = "asc";
                        } else if ("descend".equals(lower)) {
                            newOrder = "desc";
                        }
                    }
                    PageAndOrderDTO.OrderDTO orderDTO = new PageAndOrderDTO.OrderDTO();
                    orderDTO.setField(dto.getField());
                    orderDTO.setOrder(newOrder);
                    orderDTO.setSequence(dto.getSequence());
                    return orderDTO;
                }).collect(Collectors.toList());

        userQuery.setOrders(collect);

        PageDTO<AuthUserDO> doPage = authUserRepository.userPage(userQuery);
        List<UserResponseDTO> userResponseDTOList = authUserAppAssembler.toUserResponseDTOList(doPage.getRecords());

        // 2. 遍历用户，填充关联的角色和部门信息
        List<UserResponseDTO> dtoList = userResponseDTOList.stream().map(userDTO -> {
            // 2.2 填充角色信息
            // 注意：AuthRoleRepository 现有的实现是直接返回 DTO List，所以不需要 Assembler 转换
            List<AuthRoleDO> authRoleDOList = authRoleRepository.selectRoleListByUserId(userDTO.getId(), "");
            // DO->DTO
            List<RoleResponseDTO> roleDTOList = authRoleAppAssembler.toDTOList(authRoleDOList);
            userDTO.setRoleListDTO(roleDTOList);


            // 2.3 填充部门信息(要求部门新增时，入库时，所有的部门都需要入库。100-200-300，100-200-301)
            // 步骤 A: 从仓储获取 DO List (这是纯净的领域对象)
            List<AuthDeptDO> deptDOs = authDeptRepository.selectDeptListByUserId(userDTO.getId(), "");
            // 步骤 B: 使用 MapStruct 将 DO List 转换为 DTO List
            List<DeptResponseDTO> deptDTOList = authDeptAppAssembler.toDTOList(deptDOs);
            userDTO.setDeptListDTO(deptDTOList);

            //2.4 填充岗位信息
            AuthPostDO authPostDO = authPostRepository.postDynamicQuery(PostQuery.builder().id(userDTO.getPostId()).build());
            PostResponseDTO postResponseDTO = authPostAppAssembler.toDTO(authPostDO);
            userDTO.setPostDTO(postResponseDTO);

            return userDTO;
        }).collect(Collectors.toList());

        return new PageDTO<>(dtoList, doPage.getTotal());
    }
}