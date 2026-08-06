package com.mobai.mopicturebackend.exception;

import lombok.Getter;

/**
 * 错误码
 */
@Getter
public enum ResCodeEnum {

    SUCCESS("00000000", "Success"),
    FAILED("00000001", "The system is busy ,please com back later"),
    PARAMS_ERROR("400000000", "请求参数错误"),
    NOT_LOGIN_ERROR("401000001", "未登录"),
    NO_AUTH_ERROR("401000002", "无权限"),
    NOT_FOUND_ERROR("404000002", "请求数据不存在"),
    FORBIDDEN_ERROR("403000000", "禁止访问"),
    SYSTEM_ERROR("500000000", "系统内部异常"),
    OPERATION_ERROR("500000000", "操作失败");

    private final String code;

    private final String message;

    ResCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

}
