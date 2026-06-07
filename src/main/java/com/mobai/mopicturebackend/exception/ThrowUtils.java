package com.mobai.mopicturebackend.exception;

/**
 * 抛异常工具类
 * 类似于 断言 简化抛异常的代码
 */
public class ThrowUtils {

    /**
     * 条件成立则抛异常
     *
     * @param condition
     * @param runtimeException
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /**
     * 条件成立则抛异常
     *
     * @param condition
     * @param resCodeEnum
     */
    public static void throwIf(boolean condition, ResCodeEnum resCodeEnum) {
        throwIf(condition, new BusinessException(resCodeEnum));
    }

    /**
     * 条件成立则抛异常
     *
     * @param condition
     * @param resCodeEnum
     * @param message
     */
    public static void throwIf(boolean condition, ResCodeEnum resCodeEnum, String message) {
        throwIf(condition, new BusinessException(resCodeEnum, message));
    }
}
