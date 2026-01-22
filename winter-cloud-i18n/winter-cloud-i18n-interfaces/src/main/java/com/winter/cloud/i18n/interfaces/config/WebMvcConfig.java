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
import com.winter.cloud.i18n.interfaces.interceptor.TraceIdInterceptor;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
}
