package com.mobai.mopicturebackend.common;

import com.mobai.mopicturebackend.exception.BusinessException;
import com.mobai.mopicturebackend.exception.ResCodeEnum;
import lombok.Data;

import java.io.Serializable;

@Data
public class BaseResponseModel<T> implements Serializable {

    private String responseCode;

    private T data;

    private String responseMessage;

    public BaseResponseModel() {
    }

    public BaseResponseModel(String code, T data, String message) {
        this.responseCode = code;
        this.data = data;
        this.responseMessage = message;
    }

    public BaseResponseModel(String code, T data) {
        this(code, data, "");
    }

    public BaseResponseModel(ResCodeEnum resCodeEnum) {
        this(resCodeEnum.getCode(), null, resCodeEnum.getMessage());
    }

    public static <T> BaseResponseModel<T> success(T data) {
        BaseResponseModel<T> baseResponseModel = new BaseResponseModel<T>();
        baseResponseModel.setResponseCode(ResCodeEnum.SUCCESS.getCode());
        baseResponseModel.setResponseMessage(ResCodeEnum.SUCCESS.getMessage());
        baseResponseModel.setData(data);
        return baseResponseModel;
    }

    public static <T> BaseResponseModel<T> error(ResCodeEnum resCodeEnum) {
        BaseResponseModel<T> baseResponseModel = new BaseResponseModel<T>();
        baseResponseModel.setResponseCode(resCodeEnum.getCode());
        baseResponseModel.setResponseMessage(resCodeEnum.getMessage());
        return baseResponseModel;
    }

    public static <T> BaseResponseModel<T> error(BusinessException ex) {
        return new BaseResponseModel(ex.getCode(), (Object) null, ex.getMessage());
    }
}
