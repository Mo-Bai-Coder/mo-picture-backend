package com.mobai.mopicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mobai.mopicturebackend.model.dto.user.UserQueryRequest;
import com.mobai.mopicturebackend.model.entity.UserEntity;
import com.mobai.mopicturebackend.model.vo.LoginUserVO;
import com.mobai.mopicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author MoBai
 * @description 针对表【picture_user(用户)】的数据库操作Service
 * @createDate 2026-06-07 23:01:15
 */
public interface UserService extends IService<com.mobai.mopicturebackend.model.entity.UserEntity> {

    long userRegister(String userAccount, String userPassword, String checkPassword);

    String getEncryptPassword(String userPassword);

    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    LoginUserVO getLoginUserVO(com.mobai.mopicturebackend.model.entity.UserEntity user);

    com.mobai.mopicturebackend.model.entity.UserEntity getLoginUser(HttpServletRequest request);

    boolean userLogout(HttpServletRequest request);

    UserVO getUserVO(com.mobai.mopicturebackend.model.entity.UserEntity user);

    List<UserVO> getUserVOList(List<UserEntity> userList);

    QueryWrapper<UserEntity> getQueryWrapper(UserQueryRequest userQueryRequest);


}

