package com.mobai.mopicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.system.UserInfo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mobai.mopicturebackend.annotation.AuthCheck;
import com.mobai.mopicturebackend.common.BaseResponseModel;
import com.mobai.mopicturebackend.common.DeleteRequest;
import com.mobai.mopicturebackend.common.ResultUtils;
import com.mobai.mopicturebackend.constant.UserConstant;
import com.mobai.mopicturebackend.exception.BusinessException;
import com.mobai.mopicturebackend.exception.ResCodeEnum;
import com.mobai.mopicturebackend.exception.ThrowUtils;
import com.mobai.mopicturebackend.model.dto.user.UserAddRequest;
import com.mobai.mopicturebackend.model.dto.user.UserQueryRequest;
import com.mobai.mopicturebackend.model.dto.user.UserRegisterRequest;
import com.mobai.mopicturebackend.model.dto.user.UserUpdateRequest;
import com.mobai.mopicturebackend.model.entity.UserEntity;
import com.mobai.mopicturebackend.model.vo.LoginUserVO;
import com.mobai.mopicturebackend.model.vo.UserVO;
import com.mobai.mopicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户控制器
 * 提供用户注册、登录、登出、增删改查等接口
 *
 * @author mobai
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册接口
     * 新用户通过账号、密码和确认密码进行注册
     *
     * @param userRegisterRequest 用户注册请求，包含账号、密码、确认密码
     * @return 注册成功的用户ID
     */
    @PostMapping("/register")
    public BaseResponseModel<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ResCodeEnum.PARAM_ERROR);

        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录接口
     * 用户通过账号和密码登录，登录成功后会在Session中保存用户信息
     *
     * @param userRegisterRequest 用户登录请求，包含账号和密码
     * @param request             HTTP请求对象，用于存储Session信息
     * @return 登录用户的信息视图对象（脱敏后的用户信息）
     */
    @PostMapping("/login")
    public BaseResponseModel<LoginUserVO> userLogin(@RequestBody UserRegisterRequest userRegisterRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userRegisterRequest == null, ResCodeEnum.PARAM_ERROR);

        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录用户信息
     * 从Session中获取当前登录的用户信息并返回（脱敏后）
     *
     * @param request HTTP请求对象，用于从Session中获取用户信息
     * @return 当前登录用户的信息视图对象
     */
    @GetMapping("/get/login")
    public BaseResponseModel<LoginUserVO> getLoginUser(HttpServletRequest request) {
        UserEntity loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户登出接口
     * 清除Session中的用户信息，实现登出功能
     *
     * @param request HTTP请求对象，用于清除Session中的用户信息
     * @return 登出是否成功
     */
    @PostMapping("/logout")
    public BaseResponseModel<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ResCodeEnum.PARAM_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }


    /**
     * 创建用户（仅管理员可用）
     * 管理员可以手动创建新用户，默认密码为12345678
     *
     * @param userAddRequest 用户添加请求，包含用户基本信息
     * @return 新创建用户的ID
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponseModel<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ResCodeEnum.PARAM_ERROR);
        UserEntity user = new UserEntity();
        BeanUtil.copyProperties(userAddRequest, user);
        // 默认密码
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        // 插入数据库
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ResCodeEnum.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据ID获取用户信息（仅管理员可用）
     * 管理员可以通过用户ID查询完整的用户信息
     *
     * @param id 用户ID
     * @return 完整的用户实体信息
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponseModel<UserEntity> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ResCodeEnum.PARAM_ERROR);
        UserEntity user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ResCodeEnum.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据ID获取用户视图对象
     * 返回脱敏后的用户信息，适用于普通用户查看
     *
     * @param id 用户ID
     * @return 脱敏后的用户信息视图对象
     */
    @GetMapping("/get/vo")
    public BaseResponseModel<UserInfo> getUserVOById(long id) {
        BaseResponseModel<UserEntity> response = getUserById(id);
        UserEntity user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户（仅管理员可用）
     * 管理员可以通过用户ID删除用户
     *
     * @param deleteRequest 删除请求，包含要删除的用户ID
     * @return 删除是否成功
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponseModel<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ResCodeEnum.PARAM_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户信息（仅管理员可用）
     * 管理员可以修改用户的基本信息
     *
     * @param userUpdateRequest 用户更新请求，包含用户ID和需要更新的字段
     * @return 更新是否成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponseModel<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ResCodeEnum.PARAM_ERROR);
        }
        UserEntity user = new UserEntity();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ResCodeEnum.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取用户列表（仅管理员可用）
     * 管理员可以分页查询用户列表，支持条件筛选
     *
     * @param userQueryRequest 用户查询请求，包含分页参数和查询条件
     * @return 分页后的用户视图对象列表
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponseModel<Page<UserInfo>> listUserByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ResCodeEnum.PARAM_ERROR);
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        Page<UserEntity> userPage = userService.page(new Page<>(current, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

}
