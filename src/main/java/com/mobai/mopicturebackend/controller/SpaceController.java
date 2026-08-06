package com.mobai.mopicturebackend.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mobai.mopicturebackend.annotation.AuthCheck;
import com.mobai.mopicturebackend.common.BaseResponseModel;
import com.mobai.mopicturebackend.common.DeleteRequest;
import com.mobai.mopicturebackend.common.ResultUtils;
import com.mobai.mopicturebackend.constant.UserConstant;
import com.mobai.mopicturebackend.exception.BusinessException;
import com.mobai.mopicturebackend.exception.ResCodeEnum;
import com.mobai.mopicturebackend.exception.ThrowUtils;
import com.mobai.mopicturebackend.model.dto.picture.*;
import com.mobai.mopicturebackend.model.dto.space.SpaceAddRequest;
import com.mobai.mopicturebackend.model.dto.space.SpaceLevel;
import com.mobai.mopicturebackend.model.dto.space.SpaceQueryRequest;
import com.mobai.mopicturebackend.model.dto.space.SpaceUpdateRequest;
import com.mobai.mopicturebackend.model.entity.PictureEntity;
import com.mobai.mopicturebackend.model.entity.SpaceEntity;
import com.mobai.mopicturebackend.model.entity.UserEntity;
import com.mobai.mopicturebackend.model.enums.PictureReviewStatusEnum;
import com.mobai.mopicturebackend.model.enums.SpaceLevelEnum;
import com.mobai.mopicturebackend.model.vo.PictureTagCategory;
import com.mobai.mopicturebackend.model.vo.PictureVO;
import com.mobai.mopicturebackend.model.vo.SpaceVO;
import com.mobai.mopicturebackend.service.PicturePictureService;
import com.mobai.mopicturebackend.service.PictureSpaceService;
import com.mobai.mopicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/space")
public class SpaceController {

    @Resource
    private UserService userService;

    @Resource
    private PictureSpaceService spaceService;


    @PostMapping("/add")
    public BaseResponseModel<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAddRequest == null, ResCodeEnum.PARAMS_ERROR);
        UserEntity loginUser = userService.getLoginUser(request);
        long newId = spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(newId);
    }

    @PostMapping("/delete")
    public BaseResponseModel<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest
            , HttpServletRequest request) {

        //0.校验参数
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ResCodeEnum.PARAMS_ERROR);
        }
        //获取当前登录用户
        UserEntity loginUser = userService.getLoginUser(request);

        //获取入参信息
        Long id = deleteRequest.getId();

        //1.判断是否存在
        SpaceEntity oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ResCodeEnum.NOT_FOUND_ERROR);

        // 2.是否是管理员，是否是空间的 本人拥有者
        if (!oldSpace.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ResCodeEnum.NO_AUTH_ERROR);
        }
        //3.操作数据库
        boolean result = spaceService.removeById(id);
        ThrowUtils.throwIf(!result, ResCodeEnum.OPERATION_ERROR);
        return ResultUtils.success(true);
    }
    /**
     * 更新空间（仅管理员可用）
     *
     * @param spaceUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponseModel<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest,
                                             HttpServletRequest request) {

        // 1. 校验
        if (spaceUpdateRequest == null || spaceUpdateRequest.getId() <= 0) {
            throw new BusinessException(ResCodeEnum.PARAMS_ERROR);
        }
        //2.将入参和 实体类进行转换
        SpaceEntity space = new SpaceEntity();
        BeanUtils.copyProperties(spaceUpdateRequest, space);
        //3.自动根据空间等级填充数据
        spaceService.fillSpaceBySpaceLevel(space);
        //4.校验参数
        spaceService.validSpace(space, false);

        //5.判断是否存在

        //6.操作数据库
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ResCodeEnum.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取空间列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponseModel<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest,
                                                         HttpServletRequest request) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ResCodeEnum.PARAMS_ERROR);
        // 查询数据库
        Page<SpaceEntity> spacePage = spaceService.page(new Page<>(current, size),
                spaceService.getQueryWrapper(spaceQueryRequest));
        // 获取封装类
        return ResultUtils.success(spaceService.getSpaceVOPage(spacePage, request));
    }

    /**
     * 分页获取空间列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponseModel<Page<SpaceEntity>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        // 查询数据库
        Page<SpaceEntity> spacePage = spaceService.page(new Page<>(current, size),
                spaceService.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(spacePage);
    }

    /**
     * 根据 id 获取空间（封装类）
     */
    @GetMapping("/get/vo")
    public BaseResponseModel<SpaceVO> getSpaceVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ResCodeEnum.PARAMS_ERROR);
        // 查询数据库
        SpaceEntity space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ResCodeEnum.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(spaceService.getSpaceVO(space, request));
    }

    /**
     * 根据 id 获取空间（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponseModel<SpaceEntity> getSpaceById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ResCodeEnum.PARAMS_ERROR);
        // 查询数据库
        SpaceEntity space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ResCodeEnum.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(space);
    }



    /**
     * 获取空间级别列表，便于前端展示
     *
     * @return
     */
    @GetMapping("/list/level")
    public BaseResponseModel<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values())
                .map(dto -> new SpaceLevel(
                        dto.getValue(),
                        dto.getText(),
                        dto.getMaxCount(),
                        dto.getMaxSize()
                ))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }
}

