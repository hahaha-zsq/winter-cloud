package com.winter.cloud.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * ZT俱乐部网关启动类
 * 
 * <p>该类是ZT俱乐部微服务架构中的网关服务启动入口，解决了以下核心问题：</p>
 * <ul>
 *   <li>统一入口管理：为所有微服务提供统一的访问入口，避免客户端直接调用各个服务</li>
 *   <li>安全认证：在网关层实现统一的身份认证和授权，减少各服务的重复认证逻辑</li>
 *   <li>流量控制：实现流量染色、灰度发布等高级流量管理功能</li>
 *   <li>服务治理：提供负载均衡、熔断降级、监控审计等服务治理能力</li>
 * </ul>
 * 
 * <p>实现该网关服务带来的具体好处：</p>
 * <ul>
 *   <li>安全性提升：统一的认证授权机制，防止恶意请求直接访问内部服务</li>
 *   <li>运维便利：集中化的日志审计、链路追踪，便于问题排查和性能监控</li>
 *   <li>灵活部署：支持灰度发布和流量染色，降低新版本发布风险</li>
 *   <li>性能优化：智能负载均衡和请求路由，提升系统整体性能</li>
 *   <li>开发效率：各业务服务专注业务逻辑，网关统一处理横切关注点</li>
 * </ul>
 * 
 * <p>网关与其他服务的调用关系：</p>
 * <ul>
 *   <li>接收客户端请求 → 认证服务验证身份 → 路由到目标业务服务</li>
 *   <li>通过Feign客户端调用认证服务进行token验证和用户信息获取</li>
 *   <li>与注册中心交互获取服务实例信息，实现动态路由</li>
 *   <li>记录审计日志到日志系统，支持链路追踪</li>
 * </ul>
 * 
 * @author zsq
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@SpringBootApplication  // Spring Boot自动配置注解，启用自动配置机制
@EnableDiscoveryClient  // 启用服务发现客户端，支持从注册中心获取服务实例
@EnableDubbo
public class GatewayApplication {

    /**
     * 网关服务主启动方法
     * 
     * <p>该方法负责启动Spring Boot应用并输出详细的启动信息，包括：</p>
     * <ul>
     *   <li>应用访问地址（本地和外部）</li>
     *   <li>当前激活的配置文件</li>
     *   <li>应用版本信息</li>
     *   <li>网关核心功能说明</li>
     * </ul>
     * 
     * <p>实现原理：</p>
     * <ol>
     *   <li>通过SpringApplication.run()启动Spring容器</li>
     *   <li>从Environment中获取服务器配置信息（端口、SSL、上下文路径等）</li>
     *   <li>自动检测协议类型（HTTP/HTTPS）</li>
     *   <li>获取本机IP地址，构建完整的访问URL</li>
     *   <li>输出格式化的启动信息，便于运维人员查看</li>
     * </ol>
     * 
     * @param args 命令行参数，支持通过命令行传入配置参数
     * @throws UnknownHostException 当无法获取本机IP地址时抛出此异常
     */
    public static void main(String[] args) throws UnknownHostException {
        // 启动Spring Boot应用，获取应用上下文
        ConfigurableApplicationContext context = SpringApplication.run(GatewayApplication.class, args);
        // 获取环境配置信息，用于构建访问URL和输出启动信息
        Environment env = context.getEnvironment();
        
        // 自动检测协议类型：如果配置了SSL证书则使用HTTPS，否则使用HTTP
        String protocol = "http";
        if (env.getProperty("server.ssl.key-store") != null) {
            protocol = "https";
        }
        
        // 获取服务器配置信息，提供默认值以确保程序正常运行
        String serverPort = env.getProperty("server.port", "8080");  // 默认端口8080
        String contextPath = env.getProperty("server.servlet.context-path", "");  // 默认无上下文路径
        String hostAddress = InetAddress.getLocalHost().getHostAddress();  // 获取本机IP地址
        
        // 输出详细的启动信息，包括访问地址、配置文件、版本等关键信息
        log.info("\n----------------------------------------------------------\n" +
                "Application '{}' is running! Access URLs:\n" +
                "Local: \t\t{}://localhost:{}{}\n" +
                "External: \t{}://{}:{}{}\n" +
                "Profile(s): \t{}\n" +
                "Version: \t{}\n" +
                "----------------------------------------------------------",
                env.getProperty("spring.application.name", "zt-club-gateway"),
                protocol, serverPort, contextPath,
                protocol, hostAddress, serverPort, contextPath,
                env.getActiveProfiles().length == 0 ? env.getDefaultProfiles() : env.getActiveProfiles(),
                env.getProperty("app.version", "1.0.0"));
        
        // 输出网关核心功能说明，帮助开发和运维人员了解网关能力
        log.info("ZT俱乐部网关启动成功！");
        log.info("网关功能包括：");
        log.info("- 统一认证和身份验证");  // 通过AuthService和相关过滤器实现
        log.info("- 流量染色和灰度发布");  // 通过TrafficTagFilter和GrayReleaseService实现
        log.info("- 白名单和访问控制");   // 通过WhitelistService和AccessControlFilter实现
        log.info("- 服务间调用认证");     // 通过ServiceAuthFilter实现
        log.info("- 恶意脚本防护");       // 通过SecurityFilter实现XSS和SQL注入防护
        log.info("- 审计日志和链路追踪"); // 通过AuditLogFilter和AuditLogService实现
        log.info("注意：权限校验已移至具体业务服务中");  // 架构设计说明，避免网关层过重
    }
}
