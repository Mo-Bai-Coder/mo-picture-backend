package com.mobai.mopicturebackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mobai.mopicturebackend.exception.BusinessException;
import com.mobai.mopicturebackend.exception.ResCodeEnum;
import com.mobai.mopicturebackend.model.entity.PictureUser;
import com.mobai.mopicturebackend.model.enums.UserRoleEnum;
import com.mobai.mopicturebackend.service.PictureUserService;
import com.mobai.mopicturebackend.mapper.PictureUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.management.Query;

/**
 * @author MoBai
 * @description 针对表【picture_user(用户)】的数据库操作Service实现
 * @createDate 2026-06-07 23:01:15
 */
@Service
public class PictureUserServiceImpl extends ServiceImpl<PictureUserMapper, PictureUser>
        implements PictureUserService {

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ResCodeEnum.PARAM_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ResCodeEnum.PARAM_ERROR, "账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ResCodeEnum.PARAM_ERROR, "密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ResCodeEnum.PARAM_ERROR, "两次输入的密码不一致");
        }
        //2.检查账户是否重复
        QueryWrapper<PictureUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ResCodeEnum.PARAM_ERROR, "账号重复");
        }
        //3.加密
        String encryptPassword = getEncryptPassword(userPassword);
        //4.插入数据
        PictureUser pictureUser = new PictureUser();
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

    @Override
    public String getEncryptPassword(String userPassword) {
        //盐值，混淆密码
        final String SALT = "mobai";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

}




