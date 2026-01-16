package com.winter.cloud.auth.interfaces.config;

import com.winter.cloud.auth.infrastructure.config.properties.XxlJobProperties;
import com.winter.cloud.auth.interfaces.interceptor.TraceIdInterceptor;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 * <p>
 * 配置拦截器、跨域等 Web 相关设置。
 * 在 DDD 架构中，这属于接口层（interfaces）的基础设施配置。
 * </p>
 *
 * @author zsq
 */
@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final XxlJobProperties xxlJobProperties;
    private final TraceIdInterceptor traceIdInterceptor;

    public WebMvcConfig(TraceIdInterceptor traceIdInterceptor, XxlJobProperties xxlJobProperties) {
        this.traceIdInterceptor = traceIdInterceptor;
        this.xxlJobProperties = xxlJobProperties;
    }

    /**
     * 注册拦截器
     * <p>
     * 将 TraceId 拦截器注册到所有请求路径，确保每个请求都有 traceId。
     * </p>
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(traceIdInterceptor)
                .addPathPatterns("/**")  // 拦截所有请求
                .order(0);  // 设置为最高优先级，确保最先执行
    }

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info(">>>>>>>>>>> xxl-job config init.");

        // 详细的空值检查和日志
        if (xxlJobProperties == null) {
            log.error("XxlJobProperties is null, please check @EnableConfigurationProperties");
            throw new IllegalStateException("XxlJobProperties not initialized");
        }

        log.info("XxlJobProperties loaded: {}", xxlJobProperties);

        if (xxlJobProperties.getAdmin() == null) {
            log.error("XxlJobProperties.admin is null, please check xxl.job.admin configuration");
            throw new IllegalStateException("XxlJobProperties.admin not initialized");
        }

        if (xxlJobProperties.getExecutor() == null) {
            log.error("XxlJobProperties.executor is null, please check xxl.job.executor configuration");
            throw new IllegalStateException("XxlJobProperties.executor not initialized");
        }

        log.info("Admin config - addresses: {}, accessToken: {}",
                xxlJobProperties.getAdmin().getAddresses(),
                xxlJobProperties.getAdmin().getAccessToken());

        log.info("Executor config - appname: {}, ip: {}, port: {}, logpath: {}",
                xxlJobProperties.getExecutor().getAppname(),
                xxlJobProperties.getExecutor().getIp(),
                xxlJobProperties.getExecutor().getPort(),
                xxlJobProperties.getExecutor().getLogpath());

        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(xxlJobProperties.getAdmin().getAddresses());
        xxlJobSpringExecutor.setAccessToken(xxlJobProperties.getAdmin().getAccessToken());
        xxlJobSpringExecutor.setAppname(xxlJobProperties.getExecutor().getAppname());
        xxlJobSpringExecutor.setIp(xxlJobProperties.getExecutor().getIp());
        xxlJobSpringExecutor.setPort(xxlJobProperties.getExecutor().getPort());
        xxlJobSpringExecutor.setLogPath(xxlJobProperties.getExecutor().getLogpath());
        xxlJobSpringExecutor.setLogRetentionDays(xxlJobProperties.getExecutor().getLogretentiondays());

        log.info("xxl-job executor configured successfully");
        return xxlJobSpringExecutor;
    }
}
