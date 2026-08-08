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
import com.mobai.mopicturebackend.model.entity.PictureEntity;
import com.mobai.mopicturebackend.model.entity.SpaceEntity;
import com.mobai.mopicturebackend.model.entity.UserEntity;
import com.mobai.mopicturebackend.model.enums.PictureReviewStatusEnum;
import com.mobai.mopicturebackend.model.vo.PictureTagCategory;
import com.mobai.mopicturebackend.model.vo.PictureVO;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 图片管理控制器
 * 提供图片的上传、删除、更新、查询等接口功能
 *
 * @author mobai
 */
@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {

    /**
     * 本地缓存
     */
    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)
            .maximumSize(10_000L) // 最大 10000 条
            // 缓存 5 分钟后移除
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();
    /**
     * 用户服务，用于获取登录用户信息和权限校验
     */
    @Resource
    private UserService userService;
    /**
     * 图片服务，用于处理图片相关的业务逻辑
     */
    @Resource
    private PicturePictureService picturePictureService;
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private PictureSpaceService pictureSpaceService;

    /**
     * 上传图片（仅管理员可用）
     * 接收用户上传的图片文件，存储到云存储服务并返回图片信息
     *
     * @param multipartFile        上传的图片文件
     * @param pictureUploadRequest 图片上传请求参数（包含图片名称、分类等信息）
     * @param request              HTTP请求对象，用于获取当前登录用户信息
     * @return 上传成功后的图片视图对象，包含图片URL、ID等信息
     */
    @PostMapping("/upload")
    //@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponseModel<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {
        // 获取当前登录用户信息
        UserEntity loginUser = userService.getLoginUser(request);
        // 调用服务层处理图片上传逻辑
        PictureVO pictureVO = picturePictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return BaseResponseModel.success(pictureVO);
    }

    //编写图片的增删改查

    /**
     * 删除图片（仅本人和管理员可用）
     * 根据图片ID删除图片，需要验证用户权限
     *
     * @param deleteRequest 删除请求，包含要删除的图片ID
     * @param request       HTTP请求对象，用于获取当前登录用户信息
     * @return 删除结果，true表示删除成功
     */
    @PostMapping("/delete")
    public BaseResponseModel<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest
            , HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ResCodeEnum.PARAMS_ERROR);
        }
        UserEntity loginUser = userService.getLoginUser(request);
        picturePictureService.deletePicture(deleteRequest.getId(), loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片信息（仅管理员可用）
     * 更新图片的名称、描述、标签等元数据信息
     *
     * @param pictureUpdateRequest 图片更新请求，包含图片ID和要更新的字段
     * @return 更新结果，true表示更新成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponseModel<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        // 1. 校验参数是否为空
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ResCodeEnum.PARAMS_ERROR);
        }

        // 2. 将DTO转换为实体类
        // 2.1 创建实体类对象
        PictureEntity picture = new PictureEntity();
        // 2.2 将请求参数复制到实体类
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        // 2.3 将tags集合转换为JSON字符串存储
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 3. 判断图片是否存在
        Long id = pictureUpdateRequest.getId();
        PictureEntity oldPicture = picturePictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ResCodeEnum.NOT_FOUND_ERROR);
        UserEntity loginUser = userService.getLoginUser(request);
        picturePictureService.fillReviewParams(picture, loginUser);
        // 4. 执行数据库更新操作
        boolean result = picturePictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ResCodeEnum.OPERATION_ERROR);
        return BaseResponseModel.success(true);
    }

    /**
     * 根据ID获取图片详情（封装为VO对象）
     * 返回包含完整信息的图片视图对象，所有用户均可访问
     *
     * @param id      图片ID
     * @param request HTTP请求对象
     * @return 图片视图对象，包含图片的详细信息
     */
    @GetMapping("/get/vo")
    public BaseResponseModel<PictureVO> getPictureVOById(@RequestParam Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ResCodeEnum.PARAMS_ERROR);
        // 查询数据库
        PictureEntity picture = picturePictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ResCodeEnum.NOT_FOUND_ERROR);
        // 空间权限校验
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            UserEntity loginUser = userService.getLoginUser(request);
            picturePictureService.checkPictureAuth(loginUser, picture);
        }
        // 获取封装类
        return ResultUtils.success(picturePictureService.getPictureVO(picture, request));
    }
    /**
     * 分页获取图片列表（仅管理员可用）
     * 返回原始的图片实体对象分页列表，包含所有字段信息
     *
     * @param pictureQueryRequest 查询请求，包含分页参数和筛选条件
     * @return 图片实体对象的分页结果
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponseModel<Page<PictureEntity>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        // 1. 获取分页参数：当前页码和每页大小
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 2. 构建查询条件并执行分页查询
        Page<PictureEntity> picturePage = picturePictureService.page(new Page<>(current, size), picturePictureService.getQueryWrapper(pictureQueryRequest));
        // 3. 返回分页结果
        return BaseResponseModel.success(picturePage);
    }

    /**
     * 分页获取图片列表（封装为VO对象，普通用户可用）
     * 返回脱敏后的图片视图对象分页列表，限制单次最多查询20条防止爬虫
     *
     * @param pictureQueryRequest 查询请求，包含分页参数和筛选条件
     * @param request             HTTP请求对象
     * @return 图片视图对象的分页结果
     */
    @PostMapping("/list/page/vo")
    public BaseResponseModel<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ResCodeEnum.PARAMS_ERROR);
        // 空间权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId == null) {
            // 公开图库
            // 普通用户默认只能看到审核通过的数据
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            // 私有空间
            UserEntity loginUser = userService.getLoginUser(request);
            SpaceEntity space = pictureSpaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ResCodeEnum.NOT_FOUND_ERROR, "空间不存在");
            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ResCodeEnum.NO_AUTH_ERROR, "没有空间权限");
            }
        }
        // 查询数据库
        Page<PictureEntity> picturePage = picturePictureService.page(new Page<>(current, size),
                picturePictureService.getQueryWrapper(pictureQueryRequest));
        // 获取封装类
        return ResultUtils.success(picturePictureService.getPictureVOPage(picturePage, request));
    }

    /**
     * 编辑图片（普通用户可用）
     * 允许用户编辑自己上传的图片信息，管理员可编辑任意图片
     *
     * @param pictureEditRequest 图片编辑请求，包含图片ID和要编辑的字段
     * @param request            HTTP请求对象，用于获取当前登录用户信息
     * @return 编辑结果，true表示编辑成功
     */
    @PostMapping("/edit")
    public BaseResponseModel<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ResCodeEnum.PARAMS_ERROR);
        }
        UserEntity loginUser = userService.getLoginUser(request);
        picturePictureService.editPicture(pictureEditRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 获取预置标签和分类
     *
     * @return
     */
    @GetMapping("/tag_category")
    public BaseResponseModel<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        //1.构建标签列表
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        //2.构建分类列表
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setCategoryList(categoryList);
        pictureTagCategory.setTagList(tagList);
        return ResultUtils.success(pictureTagCategory);
    }

    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponseModel<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ResCodeEnum.PARAMS_ERROR);
        UserEntity loginUser = userService.getLoginUser(request);
        picturePictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    @PostMapping("/upload/url")
    public BaseResponseModel<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequest pictureUploadRequest,
                                                           HttpServletRequest request) {
        // 获取当前登录用户信息
        UserEntity loginUser = userService.getLoginUser(request);
        // 调用服务层处理图片上传逻辑
        PictureVO pictureVO = picturePictureService.uploadPicture(pictureUploadRequest.getFileUrl(), pictureUploadRequest, loginUser);
        return BaseResponseModel.success(pictureVO);
    }

    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponseModel<Integer> uploadPictureByBatch(
            @RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
            HttpServletRequest request
    ) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ResCodeEnum.PARAMS_ERROR);
        UserEntity loginUser = userService.getLoginUser(request);
        int uploadCount = picturePictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }

    @Deprecated
    @PostMapping("/list/page/vo/cache")
    public BaseResponseModel<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
//        逻辑：
//        1. 定义curren,size

        long current = pictureQueryRequest.getCurrent();
        int size = pictureQueryRequest.getPageSize();
//        2. 限制爬虫，size 不能超过20
        ThrowUtils.throwIf(size > 20, ResCodeEnum.PARAMS_ERROR);
//        3. 普通用户只能查看过审的数据
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
//        4. 构建缓存
//        a. 构建key 查询对象转json

        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());

        String cacheKey = String.format("yupicture:listPictureVOByPage:%s", hashKey);
//        b. 从redis 缓存中查询 命中返回结果

        // 1. 先从本地缓存中查询
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cachedValue != null) {
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return BaseResponseModel.success(cachedPage);
        }

        // 2. 查询Redis
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
        cachedValue = operations.get(cacheKey);
        if (cachedValue != null) {
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return BaseResponseModel.success(cachedPage);
        }
//        5. 不命中 查询数据库
//        a. 获取封装类
        Page<PictureEntity> picturePage = picturePictureService.page(new Page<>(current, size),
                picturePictureService.getQueryWrapper(pictureQueryRequest));

        Page<PictureVO> pictureVOPage = picturePictureService.getPictureVoPage(picturePage, request);

//        b. 存入Redis缓存中
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
//        c. 5 - 10 分钟随机过期，防止雪崩
        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
        // 存Redis
        operations.set(cacheKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
        //存本地缓存
        LOCAL_CACHE.put(cacheKey, cacheValue);
        log.info("缓存已更新：{}", cacheKey);
        // 获取封装类
        return ResultUtils.success(pictureVOPage);
    }


}
