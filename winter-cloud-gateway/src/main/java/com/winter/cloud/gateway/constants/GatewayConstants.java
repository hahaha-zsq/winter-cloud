package com.winter.cloud.gateway.constants;

/**
 * 网关系统公共常量类
 * 
 * <p>该类集中管理网关系统中使用的所有常量，避免魔法变量的使用，
 * 提高代码的可维护性和可读性。</p>
 * 
 * @author zsq
 * @since 1.0.0
 */
public final class GatewayConstants {

    private GatewayConstants() {
        // 私有构造函数，防止实例化
    }

    /**
     * 时间格式常量
     */
    public static class TimeFormat {
        public static final String DEFAULT_DATETIME = "yyyy-MM-dd HH:mm:ss";
        public static final String DATE_ONLY = "yyyy-MM-dd";
        public static final String TIME_ONLY = "HH:mm:ss";
        public static final String TIMESTAMP = "yyyyMMddHHmmss";
    }

    /**
     * HTTP状态码常量
     */
    public static final class HttpStatus {
        /** 成功 */
        public static final int OK = 200;
        /** 错误请求 */
        public static final int BAD_REQUEST = 400;
        /** 未授权 */
        public static final int UNAUTHORIZED = 401;
        /** 禁止访问 */
        public static final int FORBIDDEN = 403;
        /** 资源未找到 */
        public static final int NOT_FOUND = 404;
        /** 方法不允许 */
        public static final int METHOD_NOT_ALLOWED = 405;
        /** 请求过于频繁 */
        public static final int TOO_MANY_REQUESTS = 429;
        /** 内部服务器错误 */
        public static final int INTERNAL_SERVER_ERROR = 500;
        /** 服务不可用 */
        public static final int SERVICE_UNAVAILABLE = 503;
    }

    /**
     * HTTP请求头常量
     */
    public static final class Headers {
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

    /**
     * 认证相关常量
     */
    public static final class Auth {
        /** Bearer令牌前缀 */
        public static final String BEARER_PREFIX = "Bearer ";
        /** Bearer前缀长度 */
        public static final int BEARER_PREFIX_LENGTH = 7;
        /** 令牌缓存过期时间（秒） */
        public static final int TOKEN_CACHE_EXPIRE_SECONDS = 3600;
        /** 黑名单令牌键前缀 */
        public static final String BLACKLIST_TOKEN_KEY_PREFIX = "auth:blacklist:";
        /** 用户缓存键前缀 */
        public static final String USER_CACHE_KEY_PREFIX = "auth:token:";
    }

    /**
     * 白名单相关常量
     */
    public static final class Whitelist {
        /** 路径精确匹配键前缀 */
        public static final String PATH_EXACT_KEY_PREFIX = "whitelist:path:exact:";
        /** 路径模式匹配键前缀 */
        public static final String PATH_PATTERN_KEY_PREFIX = "whitelist:path:pattern:";
        /** IP白名单键前缀 */
        public static final String IP_KEY_PREFIX = "whitelist:ip:";
        /** 用户白名单键前缀 */
        public static final String USER_KEY_PREFIX = "whitelist:user:";
        /** 白名单值 */
        public static final String WHITELIST_VALUE = "true";
        /** 默认缓存过期时间（秒） */
        public static final int DEFAULT_CACHE_EXPIRE_SECONDS = 300;
    }

    /**
     * 灰度发布相关常量
     */
    public static final class Gray {
        /** 灰度白名单键前缀 */
        public static final String WHITELIST_KEY_PREFIX = "gray:whitelist:";
        /** 灰度黑名单键前缀 */
        public static final String BLACKLIST_KEY_PREFIX = "gray:blacklist:";
        /** 灰度路径支持键前缀 */
        public static final String PATH_SUPPORT_KEY_PREFIX = "gray:path:";
        /** 默认灰度版本 */
        public static final String DEFAULT_GRAY_VERSION = "v2";
        /** 默认版本标识 */
        public static final String DEFAULT_VERSION = "default";
        /** V1版本标识 */
        public static final String V1_VERSION = "v1";
        /** 默认过期时间（秒） */
        public static final long DEFAULT_EXPIRE_SECONDS = 86400L;
    }

    /**
     * 负载均衡相关常量
     */
    public static final class LoadBalancer {
        /** 权重元数据键 */
        public static final String WEIGHT_METADATA_KEY = "weight";
        /** 连接数元数据键 */
        public static final String CONNECTIONS_METADATA_KEY = "connections";
        /** 版本元数据键 */
        public static final String VERSION_METADATA_KEY = "version";
        /** 默认权重 */
        public static final int DEFAULT_WEIGHT = 1;
        /** 默认连接数 */
        public static final int DEFAULT_CONNECTIONS = 0;
        /** 随机连接数上限 */
        public static final int RANDOM_CONNECTIONS_UPPER_BOUND = 100;
    }

    /**
     * 流量标签相关常量
     */
    public static final class TrafficTag {
        /** VIP用户标签 */
        public static final String VIP_TAG = "vip";
        /** 高级用户标签 */
        public static final String PREMIUM_TAG = "premium";
        /** 标准用户标签 */
        public static final String STANDARD_TAG = "standard";
        /** 基础用户标签 */
        public static final String BASIC_TAG = "basic";
        /** 匿名用户A标签 */
        public static final String ANONYMOUS_A_TAG = "anonymous-a";
        /** 匿名用户B标签 */
        public static final String ANONYMOUS_B_TAG = "anonymous-b";
        /** 匿名用户C标签 */
        public static final String ANONYMOUS_C_TAG = "anonymous-c";
        /** VIP用户阈值 */
        public static final int VIP_THRESHOLD = 10;
        /** 高级用户阈值 */
        public static final int PREMIUM_THRESHOLD = 30;
        /** 标准用户阈值 */
        public static final int STANDARD_THRESHOLD = 60;
        /** 匿名用户A阈值 */
        public static final int ANONYMOUS_A_THRESHOLD = 20;
        /** 匿名用户B阈值 */
        public static final int ANONYMOUS_B_THRESHOLD = 50;
        /** 哈希模数 */
        public static final int HASH_MODULUS = 100;
    }

    /**
     * 安全相关常量
     */
    public static final class Security {
        /** 客户端ID格式正则表达式 */
        public static final String CLIENT_ID_PATTERN = "^[a-zA-Z0-9]{8,32}$";
        /** API版本格式正则表达式 */
        public static final String API_VERSION_PATTERN = "^v\\d+\\.\\d+$";
        /** 最大时间差（毫秒） - 10分钟 */
        public static final long MAX_TIME_DIFF_MS = 10 * 60 * 1000L;
        /** 未知IP标识 */
        public static final String UNKNOWN_IP = "unknown";
        /** IP分段数量 */
        public static final int IP_SEGMENT_COUNT = 4;
        /** IP段最小值 */
        public static final int IP_SEGMENT_MIN = 0;
        /** IP段最大值 */
        public static final int IP_SEGMENT_MAX = 255;
    }

    /**
     * 响应消息常量
     */
    public static class Messages {
        public static final String UNAUTHORIZED = "未授权访问";
        public static final String FORBIDDEN = "访问被禁止";
        public static final String INTERNAL_ERROR = "内部服务器错误";
        public static final String BAD_REQUEST = "请求参数错误";
        public static final String NOT_FOUND = "资源未找到";
        public static final String SUCCESS = "操作成功";
        public static final String FAILED = "操作失败";
        public static final String MISSING_TOKEN = "缺少认证令牌";
        public static final String TOKEN_VALIDATION_FAILED = "令牌验证失败";
    }

    /**
     * 路径相关常量
     */
    public static final class Paths {
        /** 认证登录路径 */
        public static final String AUTH_LOGIN = "/auth/login";
        /** 认证注册路径 */
        public static final String AUTH_REGISTER = "/auth/register";
        /** 监控端点路径 */
        public static final String ACTUATOR = "/actuator/**";
        /** Swagger UI路径 */
        public static final String SWAGGER_UI = "/swagger-ui/**";
        /** API文档路径 */
        public static final String API_DOCS = "/v3/api-docs/**";
        /** Web资源路径 */
        public static final String WEBJARS = "/webjars/**";
        /** 网站图标路径 */
        public static final String FAVICON = "/favicon.ico";
        /** 健康检查路径 */
        public static final String HEALTH = "/health";
        /** 信息路径 */
        public static final String INFO = "/info";
    }

    /**
     * 请求来源常量
     */
    public static final class RequestSource {
        /** Web端 */
        public static final String WEB = "web";
        /** 移动端 */
        public static final String MOBILE = "mobile";
        /** API */
        public static final String API = "api";
        /** 管理端 */
        public static final String ADMIN = "admin";
        /** 系统 */
        public static final String SYSTEM = "system";
    }

    /**
     * API版本常量
     */
    public static final class ApiVersion {
        /** 版本1.0 */
        public static final String V1_0 = "v1.0";
        /** 版本1.1 */
        public static final String V1_1 = "v1.1";
        /** 版本2.0 */
        public static final String V2_0 = "v2.0";
        /** 版本2.1 */
        public static final String V2_1 = "v2.1";
    }

    /**
     * 恶意User-Agent关键词
     */
    public static final class MaliciousUserAgents {
        /** SQL注入工具 */
        public static final String SQLMAP = "sqlmap";
        /** 网络扫描工具 */
        public static final String NMAP = "nmap";
        /** Web漏洞扫描工具 */
        public static final String NIKTO = "nikto";
        /** 端口扫描工具 */
        public static final String MASSCAN = "masscan";
    }

    /**
     * 负载均衡策略描述
     */
    public static final class LoadBalancerDescriptions {
        /** 轮询策略描述 */
        public static final String ROUND_ROBIN = "轮询策略，依次选择每个实例";
        /** 随机策略描述 */
        public static final String RANDOM = "随机策略，随机选择实例";
        /** 加权轮询策略描述 */
        public static final String WEIGHTED_ROUND_ROBIN = "加权轮询策略，根据实例权重选择";
        /** 最少连接数策略描述 */
        public static final String LEAST_CONNECTIONS = "最少连接数策略，选择连接数最少的实例";
    }

    /**
     * JSON响应字段常量
     */
    public static final class JsonFields {
        /** 错误码字段 */
        public static final String CODE = "code";
        /** 消息字段 */
        public static final String MESSAGE = "message";
        /** 数据字段 */
        public static final String DATA = "data";
        /** 时间戳字段 */
        public static final String TIMESTAMP = "timestamp";
        /** 路径字段 */
        public static final String PATH = "path";
        /** 成功标识字段 */
        public static final String SUCCESS = "success";
        /** 服务名字段 */
        public static final String SERVICE_NAME = "serviceName";
        /** 策略字段 */
        public static final String STRATEGY = "strategy";
        /** 用户ID字段 */
        public static final String USER_ID = "userId";
        /** 用户名字段 */
        public static final String USERNAME = "username";
        /** 角色字段 */
        public static final String ROLES = "roles";
        /** 权限字段 */
        public static final String PERMISSIONS = "permissions";
        /** 有效性字段 */
        public static final String VALID = "valid";
        /** 状态字段 */
        public static final String STATUS = "status";
        /** 错误字段 */
        public static final String ERROR = "error";
        /** 可用策略字段 */
        public static final String AVAILABLE_STRATEGIES = "availableStrategies";
        /** 策略描述字段 */
        public static final String DESCRIPTIONS = "descriptions";
        /** 负载均衡器工厂字段 */
        public static final String LOAD_BALANCER_FACTORY = "loadBalancerFactory";
        /** 白名单字段 */
        public static final String WHITELIST = "whitelist";
        /** 灰度字段 */
        public static final String GRAY = "gray";
        /** 审计字段 */
        public static final String AUDIT = "audit";
        /** 服务字段 */
        public static final String SERVICES = "services";
        /** 是否模式匹配字段 */
        public static final String IS_PATTERN = "isPattern";
        /** 过期时间字段 */
        public static final String EXPIRE_SECONDS = "expireSeconds";
        /** IP字段 */
        public static final String IP = "ip";
        /** 支持字段 */
        public static final String SUPPORT = "support";
    }

    /**
     * 状态常量
     */
    public static final class Status {
        /** 活跃状态 */
        public static final String ACTIVE = "active";
        /** 正常状态 */
        public static final String UP = "UP";
    }

    /**
     * 默认值常量
     */
    public static final class Defaults {
        /** 默认权重 */
        public static final String DEFAULT_WEIGHT_STR = "1";
        /** 默认连接数 */
        public static final String DEFAULT_CONNECTIONS_STR = "0";
        /** 默认过期时间 */
        public static final String DEFAULT_EXPIRE_SECONDS_STR = "0";
        /** 默认灰度过期时间 */
        public static final String DEFAULT_GRAY_EXPIRE_SECONDS_STR = "86400";
        /** 默认模式匹配 */
        public static final String DEFAULT_IS_PATTERN_STR = "false";
    }

    /**
     * 通用常量
     */
    public static class Common {
        public static final String UNKNOWN = "unknown";
        public static final String COMMA = ",";
        public static final String DOT = ".";
        public static final String SLASH = "/";
        public static final String COLON = ":";
        public static final String SEMICOLON = ";";
        public static final String EQUALS = "=";
        public static final String AMPERSAND = "&";
        public static final String QUESTION_MARK = "?";
        public static final String HASH = "#";
        public static final String SPACE = " ";
        public static final String EMPTY_STRING = "";
        public static final String TRUE = "true";
        public static final String FALSE = "false";
    }

    /**
     * 内容类型常量
     */
    public static final class ContentType {
        /** JSON内容类型 */
        public static final String APPLICATION_JSON = "application/json";
        /** JSON内容类型（带编码） */
        public static final String APPLICATION_JSON_UTF8 = "application/json;charset=UTF-8";
    }

    /**
     * 错误响应常量
     */
    public static final class ErrorResponse {
        /** 内部服务器错误响应 */
        public static final String INTERNAL_SERVER_ERROR_JSON = "{\"error\":\"Internal Server Error\"}";
        /** JSON序列化失败响应 */
        public static final String JSON_SERIALIZATION_FAILED_JSON = "{\"code\":500,\"message\":\"JSON序列化失败\"}";
    }

    /**
     * 日志消息常量
     */
    public static final class LogMessages {
        // 流量标记相关
        public static final String TRAFFIC_MARKED_AS_GRAY = "请求被标记为灰度流量";
        public static final String TRAFFIC_TAGGING_COMPLETED = "流量染色完成";
        public static final String ERROR_DETERMINING_GRAY_VERSION = "确定灰度版本时发生错误";

        // 认证相关
        public static final String REQUEST_PATH = "请求路径";
        public static final String PATH_NO_AUTH_REQUIRED = "路径 {} 无需认证，直接放行";
        public static final String PATH_MISSING_AUTH_HEADER = "请求路径 {} 缺少有效的Authorization头";
        public static final String TOKEN_VALIDATION_ERROR = "令牌验证过程中发生错误";
        public static final String TOKEN_IN_BLACKLIST = "令牌已被加入黑名单";
        public static final String GET_USER_INFO_FROM_CACHE = "从缓存获取用户信息";
        public static final String CACHE_MISS_VALIDATE_TOKEN = "缓存未命中，需要调用认证服务验证令牌";
        public static final String TOKEN_VALIDATION_EXCEPTION = "验证令牌时发生异常";
        public static final String AUTH_SERVICE_VALIDATION_FAILED = "调用认证服务验证令牌失败";
        public static final String RESPONSE_SERIALIZATION_FAILED = "序列化响应体失败";

        // 白名单相关
        public static final String WHITELIST_SERVICE_INITIALIZED = "白名单服务初始化完成";
        public static final String PATH_MATCHES_STATIC_WHITELIST = "路径匹配静态白名单";
        public static final String ERROR_CHECKING_PATH_WHITELIST = "检查路径白名单时发生错误";
        public static final String IP_MATCHES_STATIC_WHITELIST = "IP匹配静态白名单";
        public static final String ERROR_CHECKING_IP_WHITELIST = "检查IP白名单时发生错误";
        public static final String ERROR_CHECKING_USER_WHITELIST = "检查用户白名单时发生错误";
        public static final String PATH_MATCHES_DYNAMIC_EXACT_WHITELIST = "路径匹配动态精确白名单";
        public static final String PATH_MATCHES_DYNAMIC_PATTERN_WHITELIST = "路径匹配动态模式白名单";
        public static final String ERROR_CHECKING_DYNAMIC_PATH_WHITELIST = "检查动态路径白名单时发生错误";
        public static final String ERROR_CHECKING_DYNAMIC_IP_WHITELIST = "检查动态IP白名单时发生错误";
        public static final String PATH_ADDED_TO_WHITELIST = "路径已添加到白名单";
        public static final String ERROR_ADDING_PATH_TO_WHITELIST = "添加路径到白名单时发生错误";
        public static final String PATH_REMOVED_FROM_WHITELIST = "路径已从白名单中移除";
        public static final String ERROR_REMOVING_PATH_FROM_WHITELIST = "从白名单中移除路径时发生错误";
        public static final String IP_ADDED_TO_WHITELIST = "IP已添加到白名单";
        public static final String ERROR_ADDING_IP_TO_WHITELIST = "添加IP到白名单时发生错误";
        public static final String IP_REMOVED_FROM_WHITELIST = "IP已从白名单中移除";
        public static final String ERROR_REMOVING_IP_FROM_WHITELIST = "从白名单中移除IP时发生错误";
        public static final String USER_ADDED_TO_WHITELIST = "用户已添加到白名单";
        public static final String ERROR_ADDING_USER_TO_WHITELIST = "添加用户到白名单时发生错误";
        public static final String USER_REMOVED_FROM_WHITELIST = "用户已从白名单中移除";
        public static final String ERROR_REMOVING_USER_FROM_WHITELIST = "从白名单中移除用户时发生错误";

        // 灰度发布相关
        public static final String USER_IN_GRAY_WHITELIST = "用户在灰度白名单中，使用灰度版本";
        public static final String USER_IN_GRAY_BLACKLIST = "用户在灰度黑名单中，使用默认版本";
        public static final String ERROR_CHECKING_GRAY_WHITELIST = "检查灰度白名单时发生错误";
        public static final String ERROR_CHECKING_GRAY_BLACKLIST = "检查灰度黑名单时发生错误";
        public static final String ERROR_CHECKING_PATH_GRAY_SUPPORT = "检查路径灰度支持时发生错误";
        public static final String USER_ADDED_TO_GRAY_WHITELIST = "用户已添加到灰度白名单";
        public static final String ERROR_ADDING_USER_TO_GRAY_WHITELIST = "添加用户到灰度白名单时发生错误";
        public static final String USER_REMOVED_FROM_GRAY_WHITELIST = "用户已从灰度白名单中移除";
        public static final String ERROR_REMOVING_USER_FROM_GRAY_WHITELIST = "从灰度白名单中移除用户时发生错误";
        public static final String USER_ADDED_TO_GRAY_BLACKLIST = "用户已添加到灰度黑名单";
        public static final String ERROR_ADDING_USER_TO_GRAY_BLACKLIST = "添加用户到灰度黑名单时发生错误";
        public static final String USER_REMOVED_FROM_GRAY_BLACKLIST = "用户已从灰度黑名单中移除";
        public static final String ERROR_REMOVING_USER_FROM_GRAY_BLACKLIST = "从灰度黑名单中移除用户时发生错误";
        public static final String PATH_GRAY_SUPPORT_STATUS_SET = "路径灰度支持状态已设置";
        public static final String ERROR_SETTING_PATH_GRAY_SUPPORT = "设置路径灰度支持状态时发生错误";

        // 负载均衡相关
        public static final String NO_SERVICE_NAME_SKIP_GRAY_LB = "无法获取服务名，跳过灰度负载均衡";
        public static final String GRAY_LOAD_BALANCE_COMPLETED = "灰度负载均衡完成";
        public static final String NO_MATCHING_GRAY_INSTANCE = "未找到匹配的灰度服务实例";
        public static final String ERROR_GRAY_LOAD_BALANCE = "灰度负载均衡时发生错误";
        public static final String NO_SERVICE_INSTANCES_FOUND = "未找到服务实例";
        public static final String NO_GRAY_VERSION_USE_DEFAULT = "未找到灰度版本实例，使用默认版本";
        public static final String NO_MATCHING_VERSION_USE_FIRST = "未找到匹配版本的实例，使用第一个可用实例";
        public static final String ERROR_SELECTING_SERVICE_INSTANCE = "选择服务实例时发生错误";
        public static final String NO_SERVICE_INSTANCE_LIST_SUPPLIER = "No ServiceInstanceListSupplier available for service";
        public static final String NO_AVAILABLE_SERVICE_INSTANCES = "No available service instances for service";
        public static final String FAILED_TO_SELECT_SERVICE_INSTANCE = "Failed to select service instance for service";
        public static final String SELECTED_INSTANCE = "Selected instance";
        public static final String INVALID_WEIGHT_VALUE = "Invalid weight value";
        public static final String USING_DEFAULT_WEIGHT = "using default weight 1";
        public static final String INVALID_CONNECTIONS_VALUE = "Invalid connections value";
        public static final String USING_DEFAULT_CONNECTIONS = "using default connections 0";
        public static final String LOAD_BALANCE_STRATEGY_CHANGED = "Load balance strategy changed to";
        public static final String FAILED_GET_LB_STRATEGY = "Failed to get load balance strategy for service";
        public static final String INVALID_STRATEGY = "Invalid strategy";
        public static final String STRATEGY_UPDATED_SUCCESSFULLY = "Strategy updated successfully";
        public static final String LB_STRATEGY_UPDATED = "Load balance strategy updated for service";
        public static final String FAILED_SET_LB_STRATEGY = "Failed to set load balance strategy for service";
        public static final String NOT_CUSTOM_LOAD_BALANCER = "Load balancer for service {} is not a CustomLoadBalancer instance";
        public static final String FAILED_GET_LOAD_BALANCER = "Failed to get load balancer for service";

        // 访问控制相关
        public static final String ACCESS_CONTROL_CHECK_START = "访问控制检查开始";
        public static final String PATH_IN_WHITELIST_ALLOW = "路径在白名单中，允许访问";
        public static final String IP_IN_WHITELIST_ALLOW = "IP在白名单中，允许访问";
        public static final String USER_IN_WHITELIST_ALLOW = "用户在白名单中，允许访问";
        public static final String ACCESS_CONTROL_CHECK_PASSED = "访问控制检查通过，继续后续过滤器";
        public static final String ERROR_ACCESS_CONTROL_CHECK = "访问控制检查时发生错误";
        public static final String ACCESS_CONTROL_CHECK_FAILED = "访问控制检查失败";

        // 安全过滤器相关
        public static final String SECURITY_FILTER_CHECK_REQUEST = "安全过滤器检查请求";
        public static final String REQUEST_HEADER_CONTAINS_MALICIOUS = "请求头包含恶意内容";
        public static final String URL_PARAMS_CONTAIN_MALICIOUS = "URL参数包含恶意内容";
        public static final String ILLEGAL_USER_AGENT = "非法的User-Agent";
        public static final String REQUEST_TOO_FREQUENT = "请求过于频繁";
        public static final String MISSING_SECURITY_HEADERS = "缺少必要的安全标识头";
        public static final String XSS_ATTACK_IN_HEADERS = "检测到XSS攻击尝试在请求头";
        public static final String XSS_ATTACK_IN_URL_PARAMS = "检测到XSS攻击尝试在URL参数";
        public static final String MISSING_USER_AGENT = "请求缺少User-Agent头";
        public static final String MALICIOUS_USER_AGENT_DETECTED = "检测到恶意User-Agent";
        public static final String ERROR_BUILDING_ERROR_RESPONSE = "构建错误响应时发生异常";
        public static final String MISSING_CLIENT_ID = "请求缺少有效的客户端标识";
        public static final String MISSING_REQUEST_SOURCE = "请求缺少有效的来源标识";
        public static final String MISSING_API_VERSION = "请求缺少有效的API版本";
        public static final String MISSING_TIMESTAMP = "请求缺少有效的时间戳";
        public static final String TIMESTAMP_OUT_OF_RANGE = "请求时间戳超出有效范围";
        public static final String INVALID_TIMESTAMP_FORMAT = "无效的时间戳格式";

        // 审计日志相关
        public static final String EXTERNAL_SERVICE_CALL_DETECTED = "检测到外部服务调用";
    }

    /**
     * 配置键常量
     */
    public static final class ConfigKeys {
        // 白名单配置
        public static final String GATEWAY_WHITELIST_STATIC_PATHS = "gateway.whitelist.static-paths";
        public static final String GATEWAY_WHITELIST_STATIC_IPS = "gateway.whitelist.static-ips";
        public static final String GATEWAY_WHITELIST_IP_CHECK_ENABLED = "gateway.whitelist.ip-check-enabled";
        public static final String GATEWAY_WHITELIST_PATH_CHECK_ENABLED = "gateway.whitelist.path-check-enabled";
        public static final String GATEWAY_WHITELIST_CACHE_EXPIRE = "gateway.whitelist.cache-expire";

        // 灰度发布配置
        public static final String GATEWAY_GRAY_ENABLED = "gateway.gray.enabled";
        public static final String GATEWAY_GRAY_TRAFFIC_RATIO = "gateway.gray.traffic-ratio";
        public static final String GATEWAY_GRAY_VERSION = "gateway.gray.version";
        public static final String GATEWAY_GRAY_VIP_RATIO = "gateway.gray.vip-ratio";
        public static final String GATEWAY_GRAY_WHITELIST_FORCE = "gateway.gray.whitelist-force";

        // 审计日志配置
        public static final String GATEWAY_AUDIT_ENABLED = "gateway.audit.enabled";
        public static final String GATEWAY_AUDIT_LOG_REQUEST_BODY = "gateway.audit.log-request-body";
        public static final String GATEWAY_AUDIT_LOG_RESPONSE_BODY = "gateway.audit.log-response-body";
        public static final String GATEWAY_AUDIT_RETENTION_DAYS = "gateway.audit.retention-days";
        public static final String GATEWAY_AUDIT_MASK_SENSITIVE = "gateway.audit.mask-sensitive";
        public static final String GATEWAY_AUDIT_ASYNC_PROCESSING = "gateway.audit.async-processing";
    }

    /**
     * Redis键前缀常量
     */
    public static final class RedisKeys {
        // 灰度发布相关
        public static final String GRAY_PATH_SUPPORT_PREFIX = "gray:path:support:";

        // 审计日志字段
        public static final String AUDIT_TRACE_ID = "traceId";
        public static final String AUDIT_TYPE = "type";
        public static final String AUDIT_METHOD = "method";
        public static final String AUDIT_URI = "uri";
        public static final String AUDIT_CLIENT_IP = "clientIp";
        public static final String AUDIT_USER_AGENT = "userAgent";
        public static final String AUDIT_REFERER = "referer";
        public static final String AUDIT_HEADERS = "headers";
        public static final String AUDIT_QUERY_PARAMS = "queryParams";
        public static final String AUDIT_STATUS_CODE = "statusCode";
        public static final String AUDIT_PROCESSING_TIME = "processingTime";
        public static final String AUDIT_RESPONSE_HEADERS = "responseHeaders";
        public static final String AUDIT_AUTH_RESULT = "authResult";
        public static final String AUDIT_AUTH_METHOD = "authMethod";
        public static final String AUDIT_FAILURE_REASON = "failureReason";
        public static final String AUDIT_RESOURCE = "resource";
        public static final String AUDIT_ACTION = "action";
        public static final String AUDIT_RESULT = "result";
        public static final String AUDIT_EVENT_TYPE = "eventType";
        public static final String AUDIT_DESCRIPTION = "description";
        public static final String AUDIT_SEVERITY = "severity";
        public static final String AUDIT_SERVICE_NAME = "serviceName";
        public static final String AUDIT_DURATION = "duration";
        public static final String AUDIT_ERROR_MESSAGE = "errorMessage";
    }

    /**
     * 请求映射路径常量
     */
    public static final class RequestMappings {
        // 网关管理相关
        public static final String GATEWAY_MANAGEMENT = "/gateway/management";
        public static final String CONFIG = "/config";
        public static final String WHITELIST_PATH = "/whitelist/path";
        public static final String WHITELIST_IP = "/whitelist/ip";
        public static final String WHITELIST_USER = "/whitelist/user";
        public static final String GRAY_WHITELIST = "/gray/whitelist";
        public static final String GRAY_BLACKLIST = "/gray/blacklist";
        public static final String GRAY_PATH = "/gray/path";

        // 负载均衡相关
        public static final String GATEWAY_LOADBALANCER = "/gateway/loadbalancer";
        public static final String STRATEGY_SERVICE_NAME = "/strategy/{serviceName}";
        public static final String STRATEGIES = "/strategies";
    }

    /**
     * 安全相关常量
     */
    public static final class SecurityPatterns {
        // XSS攻击模式
        public static final String XSS_PATTERN = "(?i)<script[^>]*>.*?</script>|javascript:|on\\w+\\s*=|<iframe[^>]*>.*?</iframe>";
        
        // SQL注入模式
        public static final String SQL_INJECTION_PATTERN = "(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute)\\s";
        
        // IP分割符
        public static final String IP_SPLIT_PATTERN = "\\.";
        
        // UUID替换字符
        public static final String UUID_REPLACEMENT = "-";
    }

    /**
     * 路径前缀常量
     */
    public static final class PathPrefixes {
        public static final String ACTUATOR = "/actuator/";
        public static final String HEALTH_PREFIX = "/health";
        public static final String INFO_PREFIX = "/info";
    }

    /**
     * 协议常量
     */
    public static final class Protocols {
        public static final String HTTP = "http";
        public static final String HTTPS = "https";
    }
}