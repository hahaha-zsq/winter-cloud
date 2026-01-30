package com.winter.cloud.i18n.starter;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan("com.winter.cloud")
@MapperScan("com.winter.**.mapper")
@EnableDiscoveryClient
@EnableDubbo(scanBasePackages = {"com.winter.cloud.i18n"})
@Slf4j
@EnableScheduling
public class I18nApplication {

    public static void main(String[] args) {
        SpringApplication.run(I18nApplication.class, args);
        log.info("========================================");
        log.info("国际化服务启动成功！");
        log.info("========================================");
    }
}
