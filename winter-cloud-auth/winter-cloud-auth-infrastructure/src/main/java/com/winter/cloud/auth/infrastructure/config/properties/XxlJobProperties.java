package com.winter.cloud.auth.infrastructure.config.properties;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@NoArgsConstructor
@ConfigurationProperties(prefix = "xxl.job")
public class XxlJobProperties {

    private Admin admin = new Admin();
    private Executor executor = new Executor();

    @Data
    @NoArgsConstructor
    public static class Admin {
        /** 调度中心地址 */
        private String addresses;
        /** 执行器通讯 token */
        private String accessToken;
        /** 调度中心通讯超时时间，单位秒，默认3秒 */
        private int timeout = 3;
    }

    @Data
    @NoArgsConstructor
    public static class Executor {
        /** 执行器启用开关 */
        private boolean enabled = true;
        /** 执行器 appname */
        private String appname;
        /** 执行器注册地址（优先使用 address > ip:port > 自动获取） */
        private String address;
        /** 执行器 IP（可为空自动获取） */
        private String ip;
        /** 执行器端口号（<=0 自动获取，默认9999） */
        private Integer port;
        /** 执行器日志存储路径 */
        private String logpath;
        /** 执行器日志保留天数 */
        private int logretentiondays = 30;
        /** 任务扫描排除包 */
        private String excludedpackage;
    }
}