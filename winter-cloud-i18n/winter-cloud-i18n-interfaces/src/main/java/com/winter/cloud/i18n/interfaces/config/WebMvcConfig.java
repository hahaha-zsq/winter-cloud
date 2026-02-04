package com.winter.cloud.i18n.interfaces.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.winter.cloud.common.constants.CommonConstants;
import com.winter.cloud.i18n.infrastructure.config.properties.XxlJobProperties;
import com.winter.cloud.i18n.interfaces.interceptor.TraceIdInterceptor;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.HibernateValidator;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

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

    /**
     * Jackson 配置
     * <p>
     * 配置 Jackson 的 ObjectMapper，使其支持 Java 8 的时间类型（LocalDateTime 等）和旧的 Date 类型。
     * 同时，配置全局的时区和日期格式。
     * </p>
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            // =========================
            // 1. Java 8 时间类型配置 (JSR310)
            // =========================

            // LocalDateTime: yyyy-MM-dd HH:mm:ss
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(CommonConstants.DateTime.DATE_TIME_PATTERN);
            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
            builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));

            // LocalDate: yyyy-MM-dd
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(CommonConstants.DateTime.DATE_PATTERN);
            builder.serializerByType(LocalDate.class, new LocalDateSerializer(dateFormatter));
            builder.deserializerByType(LocalDate.class, new LocalDateDeserializer(dateFormatter));

            // LocalTime: HH:mm:ss
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(CommonConstants.DateTime.TIME_PATTERN);
            builder.serializerByType(LocalTime.class, new LocalTimeSerializer(timeFormatter));
            builder.deserializerByType(LocalTime.class, new LocalTimeDeserializer(timeFormatter));

            // =========================
            // 2. 老旧 Date 类型配置
            // =========================

            // 配置 java.util.Date 的格式化 (影响 Date 类型的字段)
            builder.simpleDateFormat(CommonConstants.DateTime.DATE_TIME_PATTERN);

            // =========================
            // 3. 全局通用设置
            // =========================

            // 设置全局时区 (非常重要，否则 Date 类型可能会少8小时，LocalDateTime 不受此时区影响，因为它是本地时间)
            builder.timeZone(TimeZone.getTimeZone("GMT+8"));

            // 序列化时，不将日期写为时间戳 (即不写成 [2025, 12, 30] 这种数组格式，而是写成字符串)
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            // 反序列化时，如果 JSON 中包含了 Bean 中不存在的属性，不要抛出异常
            builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            // 当后端返回数据给前端时，如果某个字段是 null，就不把这个字段写到 JSON 里（为了节省带宽或让 JSON 更干净）
            // builder.serializationInclusion(JsonInclude.Include.NON_NULL);
        };
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

    @Bean("i18bPoolExecutor")
    public ThreadPoolTaskExecutor i18bPoolExecutor() {
        //使用自定义的线程任务执行器（继承了ThreadPoolTaskExecutor类，该类最终实现了接口Executor）
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //核心线程数：线程池创建时候初始化的线程数
        executor.setCorePoolSize(20);
        //最大线程数：线程池最大的线程数，只有在缓冲队列满了之后才会申请超过核心线程数的线程
        executor.setMaxPoolSize(100);
        //缓冲队列：用来缓冲执行任务的队列
        executor.setQueueCapacity(500);
        //允许线程的空闲时间60秒：当超过了核心线程出之外的线程在空闲时间到达之后会被销毁
        executor.setKeepAliveSeconds(60);
        //线程池名的前缀：设置好了之后可以方便我们定位处理任务所在的线程池
        executor.setThreadNamePrefix("i18n-");
        //用于配置线程池在执行完所有已提交的任务后等待额外指定秒数以允许正在进行的 tasks 完成，以便在容器的其余部分继续关闭之前等待剩余的任务完成他们的执行
        executor.setAwaitTerminationSeconds(100);
        //等待所有的任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 初始化线程池
        executor.initialize();
        return executor;
    }

    /**
     * 配置校验器 (Validator)
     * <p>
     * 整合了以下功能：
     * 1. 国际化：将自定义 {@link MessageSource} 注入，使校验注解能读取数据库消息。
     * 2. 快速失败配置：配置 Hibernate Validator 的 failFast 模式（false=校验所有字段，true=遇错即停）。
     * 3. 依赖注入支持：LocalValidatorFactoryBean 默认使用 SpringConstraintValidatorFactory，
     * 自定义校验器（ConstraintValidator）中可以直接使用 @Autowired。
     *
     * @param messageSource 自定义的国际化消息源
     * @return 配置好消息源和属性的 Validator 工厂 Bean
     */
    @Bean
    public LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean validatorFactoryBean = new LocalValidatorFactoryBean();

        // 1. 设置国际化消息源 (对应您第一段代码的核心需求)
        // 这样 @Length(message="{key}") 中的占位符会被 MessageSource 解析
        validatorFactoryBean.setValidationMessageSource(messageSource);

        // 2. 配置 Hibernate Validator 特有属性 (对应您第二段代码的核心需求)
        java.util.Properties properties = new java.util.Properties();
        // failFast = false : 全量校验，返回所有错误信息
        // failFast = true  : 快速失败，遇到第一个错误就返回
        properties.setProperty("hibernate.validator.fail_fast", "false");

        validatorFactoryBean.setValidationProperties(properties);

        // 3. 关于 SpringConstraintValidatorFactory (对应您第二段代码的 beanFactory 部分)
        // LocalValidatorFactoryBean 内部默认就会自动设置 SpringConstraintValidatorFactory，
        // 所以无需手动配置 .constraintValidatorFactory(...)，自定义校验器自动支持 @Autowired。

        return validatorFactoryBean;
    }
}
