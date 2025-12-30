package com.winter.cloud.common.exception;

import com.winter.cloud.common.enums.ResultCodeEnum;
import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class BusinessException extends RuntimeException {


    //异常状态码
    private final String code;
    private String message;

    /**
     * 通过状态码和错误消息创建异常对象
     *
     * @param code    密码
     * @param message 消息
     */
    public BusinessException(String code, String message) {
        this.message = message;
        this.code = code;
    }


    /**
     * 接收枚举类型对象
     *
     * @param resultCodeEnum 结果代码枚举
     */
    public BusinessException(ResultCodeEnum resultCodeEnum) {
        this.message = resultCodeEnum.getMessage();
        this.code = resultCodeEnum.getCode();
    }
}
