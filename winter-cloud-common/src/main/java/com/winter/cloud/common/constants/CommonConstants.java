package com.winter.cloud.common.constants;

public final class CommonConstants {
    private CommonConstants() {
        // 私有构造函数，防止实例化
    }

    public static final class Redis{
        public static final String SPLIT = ":";
        /** 用户存入key为TOKEN+SPLIT+用户id，value值登录成功生成的token */
        public static final String TOKEN = "winter-cloud-token";
        /** 用户信息存入key为USER_INFO+SPLIT+用户id，value值用户信息 */
        public static final String USER_INFO = "winter-cloud-userInfo";
        public static final long EXPIRATION_TIME = 7 * 24 * 60 * 60 * 1000L;
    }

    public static final class Claim{
        public static final String NAME = "name";
    }

    public static final class Order{
        public static final String ASC = "ascend";
        public static final String DESC = "descend";
    }



    /**
     * 构建 Redis 缓存键
     * <p>
     * 用途：
     * 为用户信息生成统一格式的 Redis Key，用于缓存用户的认证信息
     * <p>
     * Key 格式：
     * winter-cloud-userInfo:12345
     * |    |    |
     * |    |    +-- 用户 ID（变量部分）
     * |    +------- 业务类型（info 表示用户信息）
     * +------------ 业务模块（user 表示用户相关）
     * <p>
     * 设计原则：
     * 1. 使用冒号分隔，符合 Redis Key 命名规范
     * 2. 分层结构清晰，便于管理和批量操作
     * 3. 常量定义在 CommonConstants 中，全局统一
     * <p>
     * 示例：
     * - userId = "12345" -> "winter-cloud-userInfo:12345"
     * - userId = "admin" -> "winter-cloud-userInfo:admin"
     *
     * @param userId 用户 ID
     * @return String Redis 缓存键，格式为 "winter-cloud-userInfo:{userId}"
     */
    public static  String buildUserCacheKey(String userId) {
        return CommonConstants.Redis.USER_INFO      // "winter-cloud-userInfo"
               + CommonConstants.Redis.SPLIT        // ":"
               + userId;                             // 用户 ID
    }

    /**
     * HTTP请求头常量
     */
    public static final class Headers {
        public static final String BEARER_PREFIX = "Bearer ";
        /** 流量标签头 */
        public static final String TRAFFIC_TAG = "X-Traffic-Tag";
        /** 链路追踪ID头 */
        public static final String TRACE_ID = "X-Trace-Id";
        /** 用户ID头 */
        public static final String USER_ID = "X-User-Id";
        /** 用户名头 */
        public static final String USERNAME = "X-Username";
        /** 用户角色头 */
        public static final String USER_ROLES = "X-User-Roles";
        /** 用户权限头 */
        public static final String USER_PERMISSIONS = "X-User-Permissions";
        /** 灰度版本头 */
        public static final String GRAY_VERSION = "X-Gray-Version";
        /** 客户端ID头 */
        public static final String CLIENT_ID = "X-Client-Id";
        /** 请求来源头 */
        public static final String REQUEST_SOURCE = "X-Request-Source";
        /** API版本头 */
        public static final String API_VERSION = "X-API-Version";
        /** 请求时间戳头 */
        public static final String REQUEST_TIMESTAMP = "X-Request-Timestamp";
        /** 授权头 */
        public static final String AUTHORIZATION = "Authorization";
        /** 用户代理头 */
        public static final String USER_AGENT = "User-Agent";
        /** 引用页头 */
        public static final String REFERER = "Referer";
        /** 内容类型头 */
        public static final String CONTENT_TYPE = "Content-Type";
        /** 转发IP头 */
        public static final String X_FORWARDED_FOR = "X-Forwarded-For";
        /** 真实IP头 */
        public static final String X_REAL_IP = "X-Real-IP";
        /** 代理客户端IP头 */
        public static final String PROXY_CLIENT_IP = "Proxy-Client-IP";
        /** WebLogic代理客户端IP头 */
        public static final String WL_PROXY_CLIENT_IP = "WL-Proxy-Client-IP";
        /** HTTP客户端IP头 */
        public static final String HTTP_CLIENT_IP = "HTTP_CLIENT_IP";
        /** HTTP转发头 */
        public static final String HTTP_X_FORWARDED_FOR = "HTTP_X_FORWARDED_FOR";
    }

}
