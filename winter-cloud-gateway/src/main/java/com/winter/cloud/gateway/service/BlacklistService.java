package com.winter.cloud.gateway.service;

import com.winter.cloud.common.constants.CommonConstants;
import com.zsq.winter.redis.ddc.service.WinterRedisTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BlacklistService {

    /** Redis操作模板，用于动态黑名单的存储和查询 */
    private final WinterRedisTemplate redisTemplate;

    /**
     * IP黑名单检查开关
     * 可通过配置动态开启或关闭IP黑名单功能
     */
    @Value("${gateway.blacklist.ip-check-enabled:true}")
    private boolean ipCheckEnabled;

    @Value("${gateway.blacklist.ip:124.43.13.123}")
    private String backlistIps;


    /**
     * 构造函数
     *
     * @param redisTemplate Redis操作模板，用于动态黑名单管理
     */
    public BlacklistService(WinterRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    public boolean isIpInBlacklist(String clientIp) {
        // 功能开关检查和参数验证
        if (!ipCheckEnabled || !StringUtils.hasText(clientIp)) {
            return false;
        }
        String[] split = backlistIps.split(",");
        Set<String> collect = Arrays.stream(split).collect(Collectors.toSet());
        try {
            // 检查静态IP黑名单（精确匹配）
            if (collect.contains(clientIp)) {
                log.debug("IP匹配静态黑名单: ip={}", clientIp);
                return true;
            }

            // 检查动态IP黑名单（Redis存储）
            return isIpInDynamicBlacklist(clientIp);

        } catch (Exception e) {
            // 异常处理：记录错误日志，返回不在黑名单中
            log.error("检查IP黑名单时发生错误: ip={}", clientIp, e);
            return false;
        }
    }


    private Boolean isIpInDynamicBlacklist(String clientIp) {
        try {
            // 构建IP黑名单缓存键并检查
            String key = CommonConstants.Redis.BLACK_IP_LIST_KEY;
            // 添加黑名单
            // redisTemplate.setAdd(key, clientIp);
            return redisTemplate.setIsMember(key, clientIp);
        } catch (Exception e) {
            // 异常处理：记录错误日志，返回不匹配
            log.error("检查动态IP黑名单时发生错误: ip={}", clientIp, e);
            return false;
        }
    }
}