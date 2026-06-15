package com.mobai.mopicturebackend.service;

import com.mobai.mopicturebackend.model.entity.PictureUser;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author MoBai
* @description 针对表【picture_user(用户)】的数据库操作Service
* @createDate 2026-06-07 23:01:15
*/
public interface PictureUserService extends IService<PictureUser> {

    /**
     * 用户注册
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return 新用户id
     */
    long userRegister(String userAccount,String userPassword,String checkPassword);

    String getEncryptPassword(String userPassword);

}
