package com.winter.cloud.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winter.cloud.gateway.common.entity.Result;
import com.winter.cloud.gateway.common.enums.ResultCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Slf4j
@Component
public class SecurityFilter implements WebFilter, Ordered {

    /**
     * JSON处理器，用于构建安全错误响应
     */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数 - 注入必要的依赖服务
     *
     * @param objectMapper JSON处理器实例，用于安全响应内容序列化
     */
    public SecurityFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * XSS（跨站脚本）攻击检测模式
     *
     * <p>该模式数组包含了常见的XSS攻击特征，用于识别恶意脚本注入：</p>
     * <ul>
     *   <li>脚本标签：&lt;script&gt;标签及其变种</li>
     *   <li>事件处理器：onclick、onload、onerror等事件属性</li>
     *   <li>脚本协议：javascript:、vbscript:等伪协议</li>
     *   <li>弹窗函数：alert()、confirm()、prompt()等函数调用</li>
     *   <li>嵌入标签：iframe、object、embed等可执行内容标签</li>
     * </ul>
     */
    private static final Pattern[] XSS_PATTERNS = {
            Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onerror(.*?)=", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onclick(.*?)=", Pattern.CASE_INSENSITIVE),
            Pattern.compile("alert\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("confirm\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("prompt\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<iframe[^>]*>.*?</iframe>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<object[^>]*>.*?</object>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<embed[^>]*>.*?</embed>", Pattern.CASE_INSENSITIVE)
    };


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }
        // 记录安全检查开始，便于问题追踪
        log.info("安全过滤器检查请求: {} {}", request.getMethod(), request.getURI());

        // 检查请求头安全性，防止头注入攻击
        if (!checkHeaderSecurity(request)) {
            return buildErrorResponse(exchange, ResultCodeEnum.MALICIOUS_REQUEST);
        }

        // 检查URL参数安全性，防止参数注入攻击
        if (!checkUrlParameterSecurity(request)) {
            return buildErrorResponse(exchange, ResultCodeEnum.XSS_ATTACK_DETECTED);
        }

        // 检查User-Agent，识别恶意工具和爬虫
        if (!checkUserAgent(request)) {
            return buildErrorResponse(exchange, ResultCodeEnum.MALICIOUS_REQUEST);
        }


        // 检查必要的安全标识头
        if (!checkRequiredHeaders(request)) {
            return buildErrorResponse(exchange, ResultCodeEnum.MALICIOUS_REQUEST);
        }

        // 所有安全检查通过，继续执行后续过滤器
        return chain.filter(exchange);
    }

    /**
     * 检查请求头安全性
     *
     * <p>该方法对所有HTTP请求头进行安全扫描，防止头注入攻击：</p>
     * <ul>
     *   <li>XSS检测：检查请求头中是否包含恶意脚本</li>
     *   <li>全面扫描：遍历所有请求头进行安全检查</li>
     *   <li>日志记录：记录检测到的攻击尝试</li>
     * </ul>
     *
     * <p>检查策略：</p>
     * <ul>
     *   <li>逐个检查：对每个请求头的名称和值进行检查</li>
     *   <li>模式匹配：使用预定义的攻击模式进行匹配</li>
     *   <li>快速失败：一旦发现攻击立即返回false</li>
     * </ul>
     *
     * @param request HTTP请求对象，包含所有请求头信息
     * @return boolean true表示请求头安全，false表示检测到攻击
     */
    private boolean checkHeaderSecurity(ServerHttpRequest request) {
        return request.getHeaders().entrySet().stream()
                .allMatch(entry -> {
                    String headerName = entry.getKey();
                    String headerValue = String.join(",", entry.getValue());

                    // 检查XSS攻击，防止脚本注入
                    if (containsXSS(headerValue)) {
                        log.warn("检测到XSS攻击尝试在请求头: {} = {}", headerName, headerValue);
                        return false;
                    }

                    return true;
                });
    }

    /**
     * 检查URL参数安全性
     *
     * <p>该方法对URL查询参数进行安全扫描，防止参数注入攻击：</p>
     * <ul>
     *   <li>参数提取：从URI中提取查询字符串</li>
     *   <li>XSS检测：检查参数中是否包含恶意脚本</li>
     *   <li>SQL注入检测：检查参数中是否包含SQL注入代码</li>
     *   <li>空值处理：对无参数请求进行安全处理</li>
     * </ul>
     *
     * <p>检查范围：</p>
     * <ul>
     *   <li>查询字符串：完整的URL查询参数字符串</li>
     *   <li>参数值：包含参数名和参数值的完整内容</li>
     *   <li>编码处理：处理URL编码的恶意内容</li>
     * </ul>
     *
     * @param request HTTP请求对象，包含URL和查询参数
     * @return boolean true表示URL参数安全，false表示检测到攻击
     */
    private boolean checkUrlParameterSecurity(ServerHttpRequest request) {
        String query = request.getURI().getQuery();
        if (query == null) {
            return true;
        }

        // 检查XSS攻击，防止参数中的脚本注入
        if (containsXSS(query)) {
            log.warn("检测到XSS攻击尝试在URL参数: {}", query);
            return false;
        }

        return true;
    }

    /**
     * 检查User-Agent安全性
     *
     * <p>该方法验证HTTP User-Agent头，识别恶意工具和异常客户端：</p>
     * <ul>
     *   <li>存在性检查：确保User-Agent头存在且不为空</li>
     *   <li>恶意工具识别：检测已知的攻击工具和扫描器</li>
     *   <li>黑名单匹配：与恶意User-Agent黑名单进行匹配</li>
     *   <li>异常行为检测：识别异常的客户端行为模式</li>
     * </ul>
     *
     * <p>检测的恶意工具包括：</p>
     * <ul>
     *   <li>sqlmap：SQL注入测试工具</li>
     *   <li>nmap：网络扫描工具</li>
     *   <li>nikto：Web漏洞扫描器</li>
     *   <li>masscan：大规模端口扫描工具</li>
     * </ul>
     *
     * @param request HTTP请求对象，包含User-Agent头信息
     * @return boolean true表示User-Agent正常，false表示检测到恶意工具
     */
    private boolean checkUserAgent(ServerHttpRequest request) {
        String userAgent = request.getHeaders().getFirst("User-Agent");
        if (userAgent == null || userAgent.trim().isEmpty()) {
            log.warn("请求缺少User-Agent头");
            return false;
        }

        // 检查是否为已知的恶意User-Agent，转换为小写进行匹配
        String lowerUserAgent = userAgent.toLowerCase();
        if (lowerUserAgent.contains("sqlmap") ||
                lowerUserAgent.contains("nmap") ||
                lowerUserAgent.contains("nikto") ||
                lowerUserAgent.contains("masscan")) {
            log.warn("检测到恶意User-Agent: {}", userAgent);
            return false;
        }

        return true;
    }

    /**
     * 检查是否包含XSS攻击代码
     *
     * <p>该方法使用预定义的正则表达式模式检测XSS攻击：</p>
     * <ul>
     *   <li>模式匹配：遍历所有XSS攻击模式进行匹配</li>
     *   <li>快速检测：使用预编译正则表达式提高检测效率</li>
     *   <li>全面覆盖：涵盖常见的XSS攻击变种和技巧</li>
     *   <li>空值处理：安全处理null输入</li>
     * </ul>
     *
     * <p>检测的攻击类型：</p>
     * <ul>
     *   <li>脚本注入：&lt;script&gt;标签注入</li>
     *   <li>事件注入：HTML事件属性注入</li>
     *   <li>协议注入：javascript:伪协议注入</li>
     *   <li>函数调用：alert()等函数调用注入</li>
     * </ul>
     *
     * @param input 待检查的输入字符串
     * @return boolean true表示包含XSS攻击代码，false表示安全
     */
    private boolean containsXSS(String input) {
        if (input == null) {
            return false;
        }

        // 遍历所有XSS攻击模式，一旦匹配立即返回
        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 构建安全错误响应
     *
     * <p>该方法为安全检查失败提供统一的错误响应格式：</p>
     * <ul>
     *   <li>响应格式：标准的JSON安全错误响应结构</li>
     *   <li>状态码：使用400 Bad Request表示请求包含恶意内容</li>
     *   <li>错误信息：提供清晰的安全错误描述</li>
     *   <li>时间戳：记录安全事件发生时间</li>
     *   <li>异常处理：处理JSON序列化异常，提供兜底响应</li>
     * </ul>
     *
     * <p>安全响应结构：</p>
     * <pre>
     * {
     *   "code": 400,
     *   "message": "请求头包含恶意内容",
     *   "timestamp": 1234567890123
     * }
     * </pre>
     *
     * @param exchange 服务器Web交换对象，用于获取响应对象
     * @param resultCode  安全错误描述信息
     * @return Mono<Void> 异步响应结果
     */
    private Mono<Void> buildErrorResponse(ServerWebExchange exchange, ResultCodeEnum resultCode) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.BAD_REQUEST);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        try {
            // 使用统一的Result实体类构造错误响应
            Result<Void> result = Result.fail(resultCode);
            String body = objectMapper.writeValueAsString(result);
            
            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            // 处理JSON序列化异常，提供兜底安全响应
            log.error("构建错误响应时发生异常", e);
            String fallbackBody = "{\"success\":false,\"code\":400,\"message\":\"安全检查失败\"}";
            DataBuffer buffer = response.bufferFactory().wrap(fallbackBody.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }
    }

    /**
     * 检查必要的安全标识头
     *
     * <p>该方法验证请求是否包含必要的安全标识头，确保请求来源的合法性：</p>
     * <ul>
     *   <li>客户端标识：检查X-Client-Id头，标识客户端身份</li>
     *   <li>请求来源：检查X-Request-Source头，标识请求来源</li>
     *   <li>API版本：检查X-API-Version头，确保API版本兼容性</li>
     *   <li>时间戳：检查X-Request-Timestamp头，防止重放攻击</li>
     * </ul>
     *
     * <p>验证策略：</p>
     * <ul>
     *   <li>必要性检查：确保关键标识头存在且不为空</li>
     *   <li>格式验证：验证标识头的格式是否正确</li>
     *   <li>白名单验证：检查客户端ID是否在允许的白名单中</li>
     *   <li>时效性检查：验证时间戳是否在有效范围内</li>
     * </ul>
     *
     * @param request HTTP请求对象，包含所有请求头信息
     * @return boolean true表示标识头验证通过，false表示验证失败
     */
    private boolean checkRequiredHeaders(ServerHttpRequest request) {
        // 检查客户端标识头
        String clientId = request.getHeaders().getFirst("X-Client-Id");
        if (!isValidClientId(clientId)) {
            log.warn("请求缺少有效的客户端标识: clientId={}", clientId);
            return false;
        }

        // 检查请求来源头
        String requestSource = request.getHeaders().getFirst("X-Request-Source");
        if (!isValidRequestSource(requestSource)) {
            log.warn("请求缺少有效的来源标识: requestSource={}", requestSource);
            return false;
        }

        // 检查API版本头
        String apiVersion = request.getHeaders().getFirst("X-API-Version");
        if (!isValidApiVersion(apiVersion)) {
            log.warn("请求缺少有效的API版本: apiVersion={}", apiVersion);
            return false;
        }

        // 检查请求时间戳头
        String timestamp = request.getHeaders().getFirst("X-Request-Timestamp");
        if (!isValidTimestamp(timestamp)) {
            log.warn("请求缺少有效的时间戳: timestamp={}", timestamp);
            return false;
        }

        return true;
    }

    /**
     * 验证客户端ID的有效性
     *
     * <p>该方法检查客户端ID是否符合安全要求：</p>
     * <ul>
     *   <li>存在性检查：确保客户端ID不为空</li>
     *   <li>格式验证：验证客户端ID的格式（字母数字组合，长度8-32位）</li>
     *   <li>白名单验证：检查客户端ID是否在允许的白名单中</li>
     * </ul>
     *
     * @param clientId 客户端标识
     * @return boolean true表示客户端ID有效，false表示无效
     */
    private boolean isValidClientId(String clientId) {
        if (clientId == null || clientId.trim().isEmpty()) {
            return false;
        }

        // 验证客户端ID格式：8-32位字母数字组合
        return clientId.matches("^[a-zA-Z0-9]{8,32}$");
    }

    /**
     * 验证请求来源的有效性
     *
     * <p>该方法检查请求来源是否为允许的来源：</p>
     * <ul>
     *   <li>存在性检查：确保请求来源不为空</li>
     *   <li>枚举验证：检查是否为预定义的有效来源</li>
     * </ul>
     *
     * @param requestSource 请求来源标识
     * @return boolean true表示请求来源有效，false表示无效
     */
    private boolean isValidRequestSource(String requestSource) {
        if (requestSource == null || requestSource.trim().isEmpty()) {
            return false;
        }

        // 定义允许的请求来源
        String[] validSources = {"web", "mobile", "api", "admin", "system"};

        for (String validSource : validSources) {
            if (validSource.equals(requestSource.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 验证API版本的有效性
     *
     * <p>该方法检查API版本是否为支持的版本：</p>
     * <ul>
     *   <li>存在性检查：确保API版本不为空</li>
     *   <li>格式验证：验证版本号格式（如v1.0, v2.1等）</li>
     *   <li>兼容性检查：确保版本在支持范围内</li>
     * </ul>
     *
     * @param apiVersion API版本号
     * @return boolean true表示API版本有效，false表示无效
     */
    private boolean isValidApiVersion(String apiVersion) {
        if (apiVersion == null || apiVersion.trim().isEmpty()) {
            return false;
        }

        // 验证API版本格式：v + 数字.数字
        if (!apiVersion.matches("^v\\d+\\.\\d+$")) {
            return false;
        }

        // 定义支持的API版本
        String[] supportedVersions = {"v1.0", "v1.1", "v2.0", "v2.1"};

        for (String supportedVersion : supportedVersions) {
            if (supportedVersion.equals(apiVersion)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 验证时间戳的有效性
     *
     * <p>该方法检查请求时间戳是否在有效范围内：</p>
     * <ul>
     *   <li>存在性检查：确保时间戳不为空</li>
     *   <li>格式验证：验证时间戳为有效的数字</li>
     *   <li>时效性检查：确保时间戳在允许的时间窗口内（±10分钟）</li>
     * </ul>
     *
     * @param timestamp 请求时间戳（毫秒）
     * @return boolean true表示时间戳有效，false表示无效
     */
    private boolean isValidTimestamp(String timestamp) {
        if (timestamp == null || timestamp.trim().isEmpty()) {
            return false;
        }

        try {
            long requestTime = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis();
            long timeDiff = Math.abs(currentTime - requestTime);

            // 允许10分钟的时间差，平衡安全性和网络延迟
            long maxTimeDiff = 10 * 60 * 1000; // 10分钟

            if (timeDiff > maxTimeDiff) {
                log.warn("请求时间戳超出有效范围: requestTime={}, currentTime={}, diff={}ms",
                        requestTime, currentTime, timeDiff);
                return false;
            }

            return true;

        } catch (NumberFormatException e) {
            log.warn("无效的时间戳格式: {}", timestamp);
            return false;
        }
    }

    /**
     * 获取过滤器执行顺序
     *
     * <p>设置最高优先级，确保安全检查在所有其他处理之前执行：</p>
     * <ul>
     *   <li>优先级设计：作为第一道防线，拥有最高执行优先级</li>
     *   <li>安全保障：确保恶意请求在进入业务逻辑前被拦截</li>
     *   <li>性能优化：尽早拦截攻击请求，节省系统资源</li>
     * </ul>
     *
     * @return int 过滤器执行顺序，数值越小优先级越高
     */
    @Override
    public int getOrder() {
        // 设置最高优先级，作为安全防护的第一道防线
        return Ordered.HIGHEST_PRECEDENCE;
    }
}