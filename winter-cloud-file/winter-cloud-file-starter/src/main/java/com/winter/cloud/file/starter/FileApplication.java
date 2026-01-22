package com.winter.cloud.file.starter;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.winter.cloud")
@MapperScan("com.winter.**.mapper")
@EnableDiscoveryClient
@EnableDubbo(scanBasePackages = {"com.winter.cloud.file"})
@Slf4j
public class FileApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileApplication.class, args);
        log.info("========================================");
        log.info("文件服务启动成功！");
        log.info("========================================");
    }
}
