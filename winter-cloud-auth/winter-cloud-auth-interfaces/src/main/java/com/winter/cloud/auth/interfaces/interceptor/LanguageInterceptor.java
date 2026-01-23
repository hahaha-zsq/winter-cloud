package com.winter.cloud.auth.interfaces.interceptor;

import com.winter.cloud.common.constants.CommonConstants;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.Locale;

/**
 * 国际化语言拦截器
 * 用于解析请求头中的语言标识并设置到全局上下文
 */
@Component
public class LanguageInterceptor implements HandlerInterceptor {

    // 如果你有自定义的请求头，例如 "lang"，也可以定义在这里
    // private static final String CUSTOM_HEADER_LANG = "lang";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 获取请求头中的语言标识
        String lang = request.getHeader(CommonConstants.Headers.LANGUAGE);
        // 兼容自定义请求头（可选）
        // if (!StringUtils.hasText(lang)) {
        //     lang = request.getHeader(CUSTOM_HEADER_LANG);
        // }

        // 2. 解析并设置到全局上下文
        if (StringUtils.hasText(lang)) {
            try {
                // 简单的 Locale 解析，支持 "en_US", "zh_CN" 等格式
                Locale locale = StringUtils.parseLocaleString(lang);
                LocaleContextHolder.setLocale(locale);
            } catch (Exception e) {
                // 如果解析失败，默认使用系统语言，避免影响主流程
                LocaleContextHolder.setLocale(Locale.getDefault());
            }
        } else {
            // 如果没有传递请求头，使用默认语言
            LocaleContextHolder.setLocale(Locale.getDefault());
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 3. 必须清理线程变量，防止内存泄漏和线程复用导致的数据污染
        LocaleContextHolder.resetLocaleContext();
    }
}