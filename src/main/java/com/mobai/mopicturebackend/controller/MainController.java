package com.mobai.mopicturebackend.controller;

import com.mobai.mopicturebackend.common.BaseResponseModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class MainController {

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public BaseResponseModel<String> health() {
        return BaseResponseModel.success("I'm ok");
    }
}
