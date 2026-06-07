package com.mobai.mopicturebackend.exception;

import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 7090242855595706359L;

    /**
     * 错误码
     */
    private final String code;

    /**
     * 通过错误码枚举构造业务异常
     *
     * @param resCodeEnum 错误码枚举
     */
    public BusinessException(ResCodeEnum resCodeEnum) {
        super(resCodeEnum.getMessage());
        this.code = resCodeEnum.getCode();
    }

    /**
     * 通过错误码枚举构造业务异常（携带自定义消息）
     *
     * @param resCodeEnum 错误码枚举
     * @param message     自定义异常消息
     */
    public BusinessException(ResCodeEnum resCodeEnum, String message) {
        super(message);
        this.code = resCodeEnum.getCode();
    }

    /**
     * 通过自定义错误码和消息构造业务异常
     *
     * @param code    错误码
     * @param message 异常消息
     */
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 通过错误码枚举构造业务异常（携带 cause）
     *
     * @param resCodeEnum 错误码枚举
     * @param cause       原始异常
     */
    public BusinessException(ResCodeEnum resCodeEnum, Throwable cause) {
        super(resCodeEnum.getMessage(), cause);
        this.code = resCodeEnum.getCode();
    }

}
