package com.mobai.mopicturebackend.controller;

import com.mobai.mopicturebackend.common.BaseResponseModel;
import com.mobai.mopicturebackend.common.ResultUtils;
import com.mobai.mopicturebackend.exception.ResCodeEnum;
import com.mobai.mopicturebackend.exception.ThrowUtils;
import com.mobai.mopicturebackend.model.dto.user.UserRegisterRequest;
import com.mobai.mopicturebackend.service.PictureUserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private PictureUserService pictureUserService;

    @PostMapping("/register")
    public BaseResponseModel<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ResCodeEnum.PARAM_ERROR);

        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = pictureUserService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }


}
