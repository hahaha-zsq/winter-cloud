package com.winter.cloud.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BlacklistService {

    /** Redis操作模板，用于动态黑名单的存储和查询 */
    private final StringRedisTemplate redisTemplate;

    /** Ant路径匹配器，用于支持通配符模式的路径匹配 */
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 静态黑名单路径配置
     * 支持Ant风格的路径模式，如/actuator/**、/health等
     */
    @Value("${gateway.Blacklist.static-paths:/actuator/**}")
    private String staticBlacklistPaths;

    /**
     * 静态黑名单IP地址配置,多个IP用逗号分隔
     */
    @Value("${gateway.blacklist.static-ips:117.23.69.244}")
    private String staticBlacklistIps;

    /**
     * IP黑名单检查开关
     * 可通过配置动态开启或关闭IP黑名单功能
     */
    @Value("${gateway.blacklist.ip-check-enabled:true}")
    private boolean ipCheckEnabled;

    /**
     * 路径黑名单检查开关
     * 可通过配置动态开启或关闭路径黑名单功能
     */
    @Value("${gateway.blacklist.path-check-enabled:true}")
    private boolean pathCheckEnabled;

    /**
     * 黑名单缓存过期时间（秒）
     * 用于控制动态黑名单在Redis中的存储时长
     */
    @Value("${gateway.blacklist.cache-expire:300}")
    private long cacheExpireSeconds;

    /** 静态路径黑名单集合，启动时初始化，提升查询性能 */
    private Set<String> staticPathSet;

    /** 静态IP黑名单集合，启动时初始化，提升查询性能 */
    private Set<String> staticIpSet;

    /**
     * 构造函数
     *
     * @param redisTemplate Redis操作模板，用于动态黑名单管理
     */
    public BlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 服务初始化方法
     *
     * <p>在Spring容器完成依赖注入后自动执行，主要完成：</p>
     * <ul>
     *   <li>解析静态黑名单配置</li>
     *   <li>初始化内存缓存集合</li>
     *   <li>记录初始化状态日志</li>
     * </ul>
     *
     * <p>该方法确保服务在接收请求前完成所有必要的初始化工作。</p>
     */
    @PostConstruct
    public void init() {
        // 初始化静态黑名单
        initStaticBlacklist();
        log.info("黑名单服务初始化完成: pathCheckEnabled={}, ipCheckEnabled={}, staticPaths={}, staticIps={}",
                pathCheckEnabled, ipCheckEnabled, staticPathSet.size(), staticIpSet.size());
    }

    /**
     * 初始化静态黑名单
     *
     * <p>该私有方法负责解析配置文件中的静态黑名单配置，并转换为内存集合。
     * 静态黑名单在服务启动时加载，运行期间不会改变，查询性能最优。</p>
     *
     * <p>处理逻辑：</p>
     * <ol>
     *   <li>解析路径配置字符串，按逗号分割</li>
     *   <li>过滤空字符串，去除首尾空格</li>
     *   <li>存储到HashSet中，提供O(1)查询性能</li>
     *   <li>同样处理IP地址配置</li>
     * </ol>
     */
    private void initStaticBlacklist() {
        // 初始化静态路径黑名单
        staticPathSet = new HashSet<>();
        if (StringUtils.hasText(staticBlacklistPaths)) {
            staticPathSet.addAll(Arrays.stream(staticBlacklistPaths.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet()));
        }

        // 初始化静态IP黑名单
        staticIpSet = new HashSet<>();
        if (StringUtils.hasText(staticBlacklistIps)) {
            staticIpSet.addAll(Arrays.stream(staticBlacklistIps.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet()));
        }
    }


    public boolean isPathInBlacklist(String path) {
        // 功能开关检查和参数验证
        if (!pathCheckEnabled || !StringUtils.hasText(path)) {
            return false;
        }
        // 先检查静态路径黑名单，没通过的话就检查动态黑名单
        try {
            // 检查静态黑名单，使用AntPathMatcher支持通配符匹配
            for (String BlacklistPath : staticPathSet) {
                if (pathMatcher.match(BlacklistPath, path)) {
                    log.debug("路径匹配静态黑名单: path={}, pattern={}", path, BlacklistPath);
                    return true;
                }
            }


            // 检查动态黑名单（Redis存储）
            return isPathInDynamicBlacklist(path);

        } catch (Exception e) {
            // 异常处理：记录错误日志，返回不在黑名单中
            log.error("检查路径黑名单时发生错误: path={}", path, e);
            return false;
        }
    }


    public boolean isIpInBlacklist(String clientIp) {
        // 功能开关检查和参数验证
        if (!ipCheckEnabled || !StringUtils.hasText(clientIp)) {
            return false;
        }

        try {
            // 检查静态IP黑名单（精确匹配）
            if (staticIpSet.contains(clientIp)) {
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


    public Boolean isUserInDynamicBlacklist(String userId) {
        // 参数验证
        if (!StringUtils.hasText(userId)) {
            return false;
        }

        try {
            // 构建用户黑名单缓存键
            String key = "blacklist:user:" + userId;
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            // 异常处理：记录错误日志，返回不在黑名单中
            log.error("检查用户黑名单时发生错误: userId={}", userId, e);
            return false;
        }
    }

    private boolean isPathInDynamicBlacklist(String path) {
        try {
            // 精确匹配检查
            String exactKey = "blacklist:path:exact:" + path;
            if (redisTemplate.hasKey(exactKey)) {
                log.debug("路径匹配动态精确黑名单: path={}", path);
                return true;
            }

            // 模式匹配检查
            Set<String> patternKeys = redisTemplate.keys("blacklist:path:pattern:*");
            for (String patternKey : patternKeys) {
                String pattern = redisTemplate.opsForValue().get(patternKey);
                if (StringUtils.hasText(pattern) && pathMatcher.match(pattern, path)) {
                    log.debug("路径匹配动态模式黑名单: path={}, pattern={}", path, pattern);
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            // 异常处理：记录错误日志，返回不匹配
            log.error("检查动态路径黑名单时发生错误: path={}", path, e);
            return false;
        }
    }


    private Boolean isIpInDynamicBlacklist(String clientIp) {
        try {
            // 构建IP黑名单缓存键并检查
            String key = "blacklist:ip:" + clientIp;
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            // 异常处理：记录错误日志，返回不匹配
            log.error("检查动态IP黑名单时发生错误: ip={}", clientIp, e);
            return false;
        }
    }
}