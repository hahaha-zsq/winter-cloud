package com.winter.cloud.auth.interfaces.exception;


import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.exception.BusinessException;
import com.winter.cloud.common.response.Response;
import com.zsq.i18n.template.WinterI18nTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

import static com.winter.cloud.common.enums.ResultCodeEnum.*;

/**
 * 全局异常处理器
 * <p>
 * 统一拦截并处理控制器层抛出的常见异常，返回规范化的 {@link Response} 数据结构。
 * 该处理器覆盖业务异常、参数绑定与校验异常、请求格式异常、请求方法异常、SQL 异常以及常见运行时异常。
 * </p>
 * <p>
 * 使用场景：当接口入参使用 {@code @Validated} / {@code @Valid}，或发生请求体解析错误、
 * 请求方式不匹配、数据库执行失败、以及运行时错误时，本类会捕获并返回一致的错误响应。
 * </p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 业务异常处理
     */
    public final WinterI18nTemplate winterI18nTemplate;

    public GlobalExceptionHandler(WinterI18nTemplate winterI18nTemplate) {
        this.winterI18nTemplate = winterI18nTemplate;
    }


    // 1. 捕获认证失败异常 (401)
    @ExceptionHandler({AuthenticationException.class})
    public Response<Void> handleAuthenticationException(AuthenticationException e) {
        log.error("捕获到认证异常: {}", e.getMessage());
        return Response.fail(UNAUTHENTICATED_LANG.getCode(),winterI18nTemplate.message(UNAUTHENTICATED_LANG.getMessage())); // 使用项目统一的 Response 结构
    }

    // 2. 捕获权限不足异常 (403)
    @ExceptionHandler({AccessDeniedException.class})
    public Response<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.error("捕获到权限异常: {}", e.getMessage());
        return Response.fail(UNAUTHORIZED_LANG.getCode(),winterI18nTemplate.message(UNAUTHORIZED_LANG.getMessage())); // 使用项目统一的 Response 结构
    }
    /**
     * 业务异常
     * <p>
     * 触发时机：当业务逻辑主动抛出 {@link BusinessException}（例如参数合法但不满足业务规则、资源状态不允许操作等）。
     * </p>
     *
     * @param e 业务异常
     * @return 标准错误响应，携带业务自定义错误码与描述
     */
    @ExceptionHandler(BusinessException.class)
    public Response<Void> handleBusinessException(BusinessException e) {
        log.error("业务异常：{}", e.getMessage(), e);
        return Response.fail(e.getCode(), e.getMessage());
    }


    /**
     * 参数绑定异常（非 JSON 场景）
     * <p>
     * 触发时机：基于表单/QueryString 参数绑定（如 {@code @ModelAttribute}、路径参数、查询参数）启用
     * {@code @Validated} 校验后，发生校验失败或类型转换失败时抛出。
     * 常见于 GET 或表单提交的参数校验错误。
     * </p>
     *
     * @param e 绑定异常
     * @return 聚合所有校验错误信息的标准错误响应
     */
    @ExceptionHandler(BindException.class)
    @ResponseBody
    public Response<?> BindExceptionHandler(BindException e) {
        StringBuilder stringBuilder = new StringBuilder();
        List<ObjectError> errors = e.getBindingResult().getAllErrors();
        for (ObjectError error : errors) {
            String message = error.getDefaultMessage();
            // 检查消息是否为国际化键格式 {key}
            if (message != null && message.startsWith("{") && message.endsWith("}")) {
                // 提取消息键
                String messageKey = message.substring(1, message.length() - 1);
                try {
                    // 尝试获取国际化消息
                    String i18nMessage = winterI18nTemplate.message(messageKey, new Object[]{}, message);
                    stringBuilder.append(i18nMessage).append("; ");
                } catch (Exception ex) {
                    // 如果获取国际化消息失败，使用原始消息
                    stringBuilder.append(message).append("; ");
                }
            } else {
                // 不是国际化键格式，直接使用原始消息
                stringBuilder.append(message).append("; ");
            }
        }
        String finalMessage = stringBuilder.toString();
        log.error(finalMessage);
        return Response.fail(ResultCodeEnum.FAIL.getCode(), finalMessage);
    }

    /**
     * 约束违反异常（方法参数级）
     * <p>
     * 触发时机：方法参数（如 {@code @PathVariable}、{@code @RequestParam}）直接标注校验注解并在类或方法上启用
     * {@code @Validated} 时，违反约束（如 {@code @NotBlank}、{@code @Min} 等）抛出。
     * </p>
     *
     * @param e 约束违反异常
     * @return 聚合所有违反约束信息的标准错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    public Response<?> ConstraintViolationExceptionHandler(ConstraintViolationException e) {
        StringBuilder stringBuilder = new StringBuilder();
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            String message = violation.getMessage();
            // 检查消息是否为国际化键格式 {key}
            if (message != null && message.startsWith("{") && message.endsWith("}")) {
                // 提取消息键
                String messageKey = message.substring(1, message.length() - 1);
                try {
                    // 尝试获取国际化消息
                    String i18nMessage = winterI18nTemplate.message(messageKey, new Object[]{}, message);
                    stringBuilder.append(i18nMessage).append("; ");
                } catch (Exception ex) {
                    // 如果获取国际化消息失败，使用原始消息
                    stringBuilder.append(message).append("; ");
                }
            } else {
                // 不是国际化键格式，直接使用原始消息
                stringBuilder.append(message).append("; ");
            }
        }
        String finalMessage = stringBuilder.toString();
        log.error(finalMessage);
        return Response.fail(ResultCodeEnum.FAIL.getCode(), finalMessage);
    }



    /**
     * 请求体校验异常（JSON Bean 校验）
     * <p>
     * 触发时机：{@code @RequestBody} 入参标注 {@code @Valid} 或 {@code @Validated}，在 Bean 校验失败时抛出。
     * 常见于 POST/PUT 的 JSON 请求体校验错误。
     * </p>
     *
     * @param e 参数校验异常
     * @return 聚合所有 Bean 字段校验错误信息的标准错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public Response<?> MethodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        StringBuilder stringBuilder = new StringBuilder();
        e.getBindingResult().getAllErrors().forEach(err -> {
            String message = err.getDefaultMessage();
            // 检查消息是否为国际化键格式 {key}
            if (message != null && message.startsWith("{") && message.endsWith("}")) {
                // 提取消息键
                String messageKey = message.substring(1, message.length() - 1);
                try {
                    // 尝试获取国际化消息
                    String i18nMessage = winterI18nTemplate.message(messageKey, new Object[]{}, message);
                    stringBuilder.append(i18nMessage).append("; ");
                } catch (Exception ex) {
                    // 如果获取国际化消息失败，使用原始消息
                    stringBuilder.append(message).append("; ");
                }
            } else {
                // 不是国际化键格式，直接使用原始消息
                stringBuilder.append(message).append("; ");
            }
        });
        String finalMessage = stringBuilder.toString();
        log.error(finalMessage);
        return Response.fail(ResultCodeEnum.FAIL_LANG.getCode(), finalMessage);
    }


    /**
     * 请求参数缺失异常
     * <p>
     * 触发时机：请求中缺少必须的 QueryString 参数或表单参数（未提供或参数名不匹配）。
     * </p>
     *
     * @param e 缺失参数异常
     * @return 标准错误响应，提示请求参数缺失
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Response<?> parameterMissingExceptionHandler(MissingServletRequestParameterException e) {
        log.error("请求参数异常", e);
        return Response.fail(ResultCodeEnum.REQUEST_PARAMETER_ERROR_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.REQUEST_PARAMETER_ERROR_LANG.getMessage(), e.getParameterName()));
    }

    /**
     * 请求体不可读/缺失异常
     * <p>
     * 触发时机：请求体缺失、JSON 语法错误、内容类型与解析器不匹配等导致无法反序列化。
     * </p>
     *
     * @param e 请求体不可读异常
     * @return 标准错误响应，提示请求体不合法或缺失
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Response<?> parameterBodyMissingExceptionHandler(HttpMessageNotReadableException e) {
        log.error("参数体不能为空", e);
        return Response.fail(ResultCodeEnum.BODY_PARAMETER_ERROR_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.BODY_PARAMETER_ERROR_LANG.getMessage()));

    }

    /**
     * 请求方式不支持异常
     * <p>
     * 触发时机：客户端使用了未在控制器映射中声明的 HTTP 方法（如用 POST 调用仅支持 GET 的接口）。
     * </p>
     *
     * @param e 方法不支持异常
     * @return 标准错误响应，提示请求方法错误
     */
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
    public Response<?> handleException(HttpRequestMethodNotSupportedException e) {
        log.error(e.getMessage(), e);
        return Response.fail(ResultCodeEnum.METHOD_ERROR_LANG.getCode(), winterI18nTemplate.message(ResultCodeEnum.METHOD_ERROR_LANG.getMessage(), e.getMethod()));
    }


    /**
     * SQL 执行异常
     * <p>
     * 触发时机：数据库访问过程中发生错误（如语法错误、连接失败、约束冲突等）。
     * </p>
     *
     * @param e SQL 异常
     * @return 标准错误响应，提示 SQL 执行异常
     */
    @ExceptionHandler(SQLException.class)
    public Response<?> handleSQLException(SQLException e) {
        log.error("SQL执行异常: {}", e.getMessage(), e);
        return Response.fail(ResultCodeEnum.FAIL.getCode(), "SQL执行异常");
    }

    /**
     * 常见运行时异常处理
     * <p>
     * 触发时机：出现空指针、数组/列表越界、类型转换错误等常见运行时异常。
     * </p>
     *
     * @param e 运行时异常
     * @return 标准错误响应，提示系统内部错误
     */
    @ExceptionHandler({NullPointerException.class, IndexOutOfBoundsException.class, ClassCastException.class})
    public Response<?> handleNullPointerException(RuntimeException e) {
        log.error("运行时异常", e);
        return Response.fail(ResultCodeEnum.FAIL.getCode(), "系统内部错误");
    }

    /**
     * SQL 完整性约束违反异常
     * <p>
     * 触发时机：当数据库操作违反完整性约束时抛出，如唯一约束冲突、外键约束冲突等。
     * </p>
     *
     * @param e SQL 完整性约束违反异常
     * @return 标准错误响应，提示违反数据库约束
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException .class)
    public Response<Void> handleSQLIntegrityConstraintViolationException(SQLIntegrityConstraintViolationException e) {
        log.error("数据库完整性约束违反异常: {}", e.getMessage(), e);
        return Response.fail(ResultCodeEnum.FAIL.getCode(), "数据操作违反数据库约束");
    }


}
