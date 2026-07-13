package com.mobai.mopicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mobai.mopicturebackend.constant.UserConstant;
import com.mobai.mopicturebackend.exception.BusinessException;
import com.mobai.mopicturebackend.exception.ResCodeEnum;
import com.mobai.mopicturebackend.mapper.UserMapper;
import com.mobai.mopicturebackend.model.dto.user.UserQueryRequest;
import com.mobai.mopicturebackend.model.entity.UserEntity;
import com.mobai.mopicturebackend.model.enums.UserRoleEnum;
import com.mobai.mopicturebackend.model.vo.LoginUserVO;
import com.mobai.mopicturebackend.model.vo.UserVO;
import com.mobai.mopicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.swing.text.html.parser.Entity;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 * 提供用户注册、登录、登出、查询等核心业务逻辑
 *
 * @author MoBai
 * @description 针对表【picture_user(用户)】的数据库操作Service实现
 * @createDate 2026-06-07 23:01:15
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    /**
     * 用户注册
     * 对账号密码进行校验，检查账号是否重复，加密密码后存入数据库
     *
     * @param userAccount   用户账号，长度至少4位
     * @param userPassword  用户密码，长度至少8位
     * @param checkPassword 确认密码，需与密码一致
     * @return 注册成功的用户ID
     * @throws BusinessException 参数校验失败或数据库操作失败时抛出
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 参数校验：检查是否为空、长度是否符合要求、两次密码是否一致
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ResCodeEnum.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ResCodeEnum.PARAMS_ERROR, "账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ResCodeEnum.PARAMS_ERROR, "密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ResCodeEnum.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 2. 检查账户是否已存在，避免重复注册
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ResCodeEnum.PARAMS_ERROR, "账号重复");
        }
        // 3. 密码加密：使用MD5加盐值的方式加密密码
        String encryptPassword = getEncryptPassword(userPassword);
        // 4. 创建用户对象并设置默认值，插入数据库
        UserEntity pictureUser = new UserEntity();
        pictureUser.setUserAccount(userAccount);
        pictureUser.setUserPassword(encryptPassword);
        pictureUser.setUserName("普通用9527");
        pictureUser.setUserRole(UserRoleEnum.USER.getRoleValue());
        boolean saveResult = this.save(pictureUser);
        if (!saveResult) {
            throw new BusinessException(ResCodeEnum.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        return pictureUser.getId();
    }

    /**
     * 密码加密方法
     * 使用MD5算法配合固定盐值对密码进行加密
     *
     * @param userPassword 原始密码
     * @return 加密后的密码字符串
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        // 盐值，用于混淆密码，增加破解难度
        final String SALT = "mobai";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    /**
     * 用户登录
     * 校验账号密码，验证用户是否存在，登录成功后将用户信息存入Session
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @param request      HTTP请求对象，用于存储Session信息
     * @return 脱敏后的登录用户信息
     * @throws BusinessException 参数校验失败或用户不存在时抛出
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 参数校验：检查账号密码是否为空、长度是否符合要求
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ResCodeEnum.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ResCodeEnum.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ResCodeEnum.PARAMS_ERROR, "密码错误");
        }

        // 2. 密码加密后查询数据库，验证用户是否存在
        String encryptPassword = getEncryptPassword(userPassword);
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        UserEntity user = this.baseMapper.selectOne(queryWrapper);
        if (user == null) {
            log.info("用户不存在");
            throw new BusinessException(ResCodeEnum.PARAMS_ERROR, "用户不存在");
        }
        // 3. 将用户信息存入Session，保持登录状态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);

        return this.getLoginUserVO(user);
    }

    /**
     * 获取脱敏类的用户信息
     *
     * @param user 用户
     * @return 脱敏后的用户信息
     */
    @Override
    public LoginUserVO getLoginUserVO(UserEntity user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 获取当前登录用户信息
     * 从Session中获取用户信息，如果未登录则抛出异常
     *
     * @param request HTTP请求对象，用于从Session中获取用户信息
     * @return 当前登录的用户实体对象
     * @throws BusinessException 用户未登录时抛出
     */
    @Override
    public UserEntity getLoginUser(HttpServletRequest request) {
        Object userObject = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        UserEntity pictureUser = (UserEntity) userObject;
        if (pictureUser == null) {
            throw new BusinessException(ResCodeEnum.NOT_LOGIN_ERROR);
        }
        return pictureUser;
    }

    /**
     * 用户登出
     * 清除Session中的用户登录状态，实现登出功能
     *
     * @param request HTTP请求对象，用于清除Session中的用户信息
     * @return 登出成功返回true
     * @throws BusinessException 用户未登录时抛出
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 1. 判断用户是否已登录
        Object userObject = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (userObject == null) {
            throw new BusinessException(ResCodeEnum.NOT_LOGIN_ERROR);
        }
        // 2. 移除Session中的登录状态，完成登出
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);

        return true;
    }

    /**
     * 获取脱敏后的用户视图对象
     * 将用户实体转换为不包含敏感信息的视图对象
     *
     * @param user 用户实体对象
     * @return 脱敏后的用户视图对象，如果用户为空则返回null
     */
    @Override
    public UserVO getUserVO(UserEntity user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 构建用户查询条件
     * 根据查询请求中的条件动态构建QueryWrapper，支持多条件组合查询和排序
     *
     * @param userQueryRequest 用户查询请求，包含各种查询条件和排序参数
     * @return 构建好的查询条件包装器
     * @throws BusinessException 请求参数为空时抛出
     */
    @Override
    public QueryWrapper<UserEntity> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ResCodeEnum.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public boolean isAdmin(UserEntity user) {
        return user != null && UserConstant.ADMIN_ROLE.equals(user.getUserRole());
    }

    /**
     * 批量获取脱敏后的用户视图列表
     * 将用户实体列表转换为用户视图对象列表，过滤敏感信息
     *
     * @param userList 用户实体列表
     * @return 脱敏后的用户视图对象列表，如果输入为空则返回空列表
     */
    @Override
    public List<UserVO> getUserVOList(List<UserEntity> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

}




