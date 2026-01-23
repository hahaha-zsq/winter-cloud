package com.winter.cloud.dict.interfaces.filter;

import com.winter.cloud.common.constants.CommonConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;

/**
 * ❌ 语言拦截器放在 HandlerInterceptor → 全局异常里会丢
 * ✅ 语言处理必须放在 Filter → 全链路可用
 */
@Order(-1)
@Component
@Slf4j
public class LanguageFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String lang = request.getHeader(CommonConstants.Headers.LANGUAGE);

            if (StringUtils.hasText(lang)) {
                Locale locale = StringUtils.parseLocaleString(lang);
                LocaleContextHolder.setLocale(locale);
            } else {
                LocaleContextHolder.setLocale(Locale.getDefault());
            }
            log.info("LanguageFilter executed, locale={}", LocaleContextHolder.getLocale());

            filterChain.doFilter(request, response);

        } finally {
            // 一定清理
            LocaleContextHolder.resetLocaleContext();
        }
    }
}