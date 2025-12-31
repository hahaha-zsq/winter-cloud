package com.winter.cloud.auth.interfaces.config;

import com.winter.cloud.auth.interfaces.interceptor.TraceIdInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
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
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TraceIdInterceptor traceIdInterceptor;

    public WebMvcConfig(TraceIdInterceptor traceIdInterceptor) {
        this.traceIdInterceptor = traceIdInterceptor;
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

    /**
     * 配置全局跨域
     * <p>
     * 允许所有来源的跨域请求，支持常用的 HTTP 方法和请求头。
     * 生产环境建议根据实际需求限制允许的来源。
     * </p>
     */
    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")  // 允许所有来源，生产环境建议配置具体域名
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // 允许的 HTTP 方法
                .allowedHeaders("*")  // 允许所有请求头
                .allowCredentials(true)  // 允许携带凭证（如 Cookie）
                .maxAge(3600);  // 预检请求的缓存时间（秒）
    }
}
