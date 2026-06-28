package com.mobai.mopicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mobai.mopicturebackend.exception.BusinessException;
import com.mobai.mopicturebackend.model.dto.user.UserQueryRequest;
import com.mobai.mopicturebackend.model.entity.UserEntity;
import com.mobai.mopicturebackend.model.vo.LoginUserVO;
import com.mobai.mopicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户服务接口
 * 提供用户注册、登录、登出、查询等核心业务功能
 *
 * @author MoBai
 * @description 针对表【picture_user(用户)】的数据库操作Service
 * @createDate 2026-06-07 23:01:15
 */
public interface UserService extends IService<UserEntity> {

    /**
     * 用户注册
     * 
     * @param userAccount 用户账号
     * @param userPassword 用户密码
     * @param checkPassword 确认密码，需与userPassword一致
     * @return 新用户的ID
     * @throws BusinessException 当账号已存在、密码不一致或参数不合法时抛出异常
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 对用户密码进行加密处理
     * 使用加盐加密算法，确保密码安全性
     * 
     * @param userPassword 原始密码
     * @return 加密后的密码字符串
     */
    String getEncryptPassword(String userPassword);

    /**
     * 用户登录
     * 验证用户账号和密码，登录成功后将用户信息存入Session
     * 
     * @param userAccount 用户账号
     * @param userPassword 用户密码
     * @param request HTTP请求对象，用于获取Session
     * @return 登录用户的信息VO对象
     * @throws BusinessException 当账号不存在、密码错误或参数不合法时抛出异常
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 将用户实体转换为登录用户VO对象
     * 脱敏处理，不返回密码等敏感信息
     * 
     * @param user 用户实体对象
     * @return 脱敏后的登录用户VO对象
     */
    LoginUserVO getLoginUserVO(UserEntity user);

    /**
     * 从Session中获取当前登录用户
     * 
     * @param request HTTP请求对象，用于获取Session中的用户信息
     * @return 当前登录的用户实体对象
     * @throws BusinessException 当用户未登录时抛出异常
     */
    UserEntity getLoginUser(HttpServletRequest request);

    /**
     * 用户登出
     * 清除Session中的用户信息
     * 
     * @param request HTTP请求对象，用于清除Session
     * @return 登出是否成功
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 将用户实体转换为用户VO对象
     * 用于向前端返回脱敏后的用户信息
     * 
     * @param user 用户实体对象
     * @return 脱敏后的用户VO对象
     */
    UserVO getUserVO(UserEntity user);

    /**
     * 批量将用户实体列表转换为用户VO列表
     * 
     * @param userList 用户实体列表
     * @return 脱敏后的用户VO列表
     */
    List<UserVO> getUserVOList(List<UserEntity> userList);

    /**
     * 根据查询请求构建MyBatis-Plus的QueryWrapper查询条件
     * 支持按账号、用户名、性别、用户角色等多条件组合查询
     * 
     * @param userQueryRequest 用户查询请求对象，包含各种查询条件
     * @return 构建好的QueryWrapper查询条件对象
     */
    QueryWrapper<UserEntity> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(UserEntity user);


}

