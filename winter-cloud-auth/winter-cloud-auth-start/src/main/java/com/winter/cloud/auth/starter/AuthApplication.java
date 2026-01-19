package com.winter.cloud.auth.starter;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.mybatis.spring.annotation.MapperScan;

/**
 * 认证服务启动类
 */
@SpringBootApplication
@ComponentScan("com.winter.cloud")
@MapperScan("com.winter.**.mapper")
@EnableDiscoveryClient
@EnableDubbo(scanBasePackages = {"com.winter.cloud.auth"})
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
