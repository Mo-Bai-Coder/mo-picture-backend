package com.mobai.mopicturebackend.exception;

import com.mobai.mopicturebackend.common.BaseResponseModel;
import com.mobai.mopicturebackend.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponseModel<?> bussinessExceptionHandler(BusinessException e) {
        // 日志里打印完整堆栈（包含 cause）
        log.error("业务异常 code={}, message={}", e.getCode(), e.getMessage(), e);

        // 返回给前端的只暴露 code 和 message，不暴露堆栈
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponseModel<?> runtimeExceptionHandler(RuntimeException e) {
        // 日志里打印完整堆栈（包含 cause）
        log.error("RuntimeException message={},error={} ", e.getMessage(), e);

        // 返回给前端的只暴露 code 和 message，不暴露堆栈
        return ResultUtils.error(ResCodeEnum.FAILED.getCode(), ResCodeEnum.FAILED.getMessage());
    }


}
