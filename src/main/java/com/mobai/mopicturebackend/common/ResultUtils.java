package com.mobai.mopicturebackend.common;

import com.mobai.mopicturebackend.exception.ResCodeEnum;

public class ResultUtils {

    public static BaseResponseModel success(Object data) {
        return BaseResponseModel.success(data);
    }

    public static BaseResponseModel error(ResCodeEnum errorCode) {
        return BaseResponseModel.error(errorCode);
    }

    public static BaseResponseModel error(String errorCode, String message) {
        return new BaseResponseModel(errorCode, null, message);
    }

    public static BaseResponseModel error(ResCodeEnum errorCode, String message) {
        return new BaseResponseModel(errorCode.getCode(), null, message);
    }

}
