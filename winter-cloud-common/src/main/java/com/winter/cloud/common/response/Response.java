package com.winter.cloud.common.response;


import cn.hutool.core.util.ObjectUtil;
import com.winter.cloud.common.enums.ResultCodeEnum;
import com.winter.cloud.common.util.Context;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应对象
 * <p>
 * 在 DDD 架构中，这是通用层（common）的基础设施组件，
 * 用于封装统一的响应格式，包含业务数据、状态码、消息和链路追踪ID。
 * 作为通用组件，可被 interfaces 层和 api 层共同使用。
 * </p>
 *
 * @param <T> 响应数据类型
 * @author zsq
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> implements Serializable {

    private static final long serialVersionUID = 7000723935764546321L;

    /**
     * 响应状态码
     */
    private String code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 链路追踪ID
     * 从 ThreadLocal 上下文中自动获取，用于分布式追踪
     */
    private String traceId;

    /**
     * 构建响应对象
     * <p>
     * 自动从上下文中获取 traceId，确保每个响应都包含追踪信息。
     * </p>
     *
     * @param data    响应数据
     * @param code    状态码
     * @param message 响应消息
     * @param <T>     数据类型
     * @return 响应对象
     */
    public static <T> Response<T> build(T data, String code, String message) {
        Response<T> response = new Response<>();

        // 设置数据（如果不为空）
        if (!ObjectUtil.isEmpty(data)) {
            response.setData(data);
        }

        // 设置状态码和消息
        response.setCode(code);
        response.setMessage(message);

        // 从上下文中获取 traceId
        response.setTraceId(Context.getTraceId());

        return response;
    }

    /**
     * 使用结果码枚举构建响应对象
     * <p>
     * 根据结果码枚举设置状态码和消息，并自动从上下文中获取 traceId
     * </p>
     *
     * @param data           响应数据
     * @param resultCodeEnum 结果码枚举
     * @param <T>            数据类型
     * @return 响应对象
     */
    public static <T> Response<T> build(T data, ResultCodeEnum resultCodeEnum) {
        //创建Response对象，设置值，返回对象
        Response<T> response = new Response<>();
        //判断返回结果中是否需要数据
        if (!ObjectUtil.isEmpty(data)) {
            //设置数据到Response对象
            response.setData(data);
        }
        //设置其他值
        response.setCode(resultCodeEnum.getCode());
        response.setMessage(resultCodeEnum.getMessage());
        //设置traceId
        response.setTraceId(Context.getTraceId());
        //返回设置值之后的对象
        return response;
    }

    /**
     * 构建成功响应对象
     * <p>
     * 使用 SUCCESS 状态码和消息构建响应对象，包含指定的数据
     * </p>
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功响应对象
     */
    public static <T> Response<T> ok(T data) {
        return ok(ResultCodeEnum.SUCCESS.getCode(), ResultCodeEnum.SUCCESS.getMessage(), data);
    }

    public static <T> Response<T> ok(ResultCodeEnum resultCodeEnum, T data) {
        return ok(resultCodeEnum.getCode(), resultCodeEnum.getMessage(), data);
    }

    public static <T> Response<T> ok(String code, String message, T data) {
        return build(data, code, message);
    }

    public static <T> Response<T> ok() {
        return ok(null);
    }


    /**
     * 构建失败响应对象
     * <p>
     * 使用指定的状态码和消息构建失败响应对象
     * </p>
     *
     * @param code    状态码
     * @param message 响应消息
     * @param <T>     数据类型
     * @return 失败响应对象
     */
    public static <T> Response<T> fail(String code, String message) {
        //创建Response对象，设置值，返回对象
        Response<T> response = new Response<>();
        //设置其他值
        response.setCode(code);
        response.setMessage(message);
        //设置traceId
        response.setTraceId(Context.getTraceId());
        return response;
    }

    /**
     * 构建默认失败响应对象
     * <p>
     * 使用 FAIL 状态码和消息构建失败响应对象
     * </p>
     *
     * @param <T> 数据类型
     * @return 失败响应对象
     */
    public static <T> Response<T> fail() {
        return fail(ResultCodeEnum.FAIL.getCode(), ResultCodeEnum.FAIL.getMessage());
    }

    /**
     * 构建失败响应对象
     * <p>
     * 使用指定的结果码枚举构建失败响应对象
     * </p>
     *
     * @param resultCodeEnum 结果码枚举
     * @param <T>            数据类型
     * @return 失败响应对象
     */
    public static <T> Response<T> fail(ResultCodeEnum resultCodeEnum) {
        return fail(resultCodeEnum.getCode(), resultCodeEnum.getMessage());
    }
}