package com.mobai.mopicturebackend.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mobai.mopicturebackend.annotation.AuthCheck;
import com.mobai.mopicturebackend.common.BaseResponseModel;
import com.mobai.mopicturebackend.common.DeleteRequest;
import com.mobai.mopicturebackend.common.ResultUtils;
import com.mobai.mopicturebackend.constant.UserConstant;
import com.mobai.mopicturebackend.exception.BusinessException;
import com.mobai.mopicturebackend.exception.ResCodeEnum;
import com.mobai.mopicturebackend.exception.ThrowUtils;
import com.mobai.mopicturebackend.model.dto.picture.PictureEditRequest;
import com.mobai.mopicturebackend.model.dto.picture.PictureQueryRequest;
import com.mobai.mopicturebackend.model.dto.picture.PictureUpdateRequest;
import com.mobai.mopicturebackend.model.dto.picture.PictureUploadRequest;
import com.mobai.mopicturebackend.model.entity.PictureEntity;
import com.mobai.mopicturebackend.model.entity.UserEntity;
import com.mobai.mopicturebackend.model.vo.PictureTagCategory;
import com.mobai.mopicturebackend.model.vo.PictureVO;
import com.mobai.mopicturebackend.service.PicturePictureService;
import com.mobai.mopicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
     * 用户服务，用于获取登录用户信息和权限校验
     */
    @Resource
    private UserService userService;

    /**
     * 图片服务，用于处理图片相关的业务逻辑
     */
    @Resource
    private PicturePictureService picturePictureService;

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
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
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
    public BaseResponseModel<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        // 1. 校验参数是否为空
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ResCodeEnum.PARAM_ERROR);
        }
        // 2. 获取当前登录人信息
        UserEntity loginUser = userService.getLoginUser(request);
        // 3. 获取请求参数中的图片ID
        long id = deleteRequest.getId();
        // 4. 判断图片是否存在
        PictureEntity picture = picturePictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ResCodeEnum.NOT_FOUND_ERROR);

        // 5. 权限校验：仅本人和管理员可以删除
        if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ResCodeEnum.NOT_AUTH_ERROR, "仅本人和管理者有权限删除");
        }

        // 6. 执行数据库删除操作
        boolean result = picturePictureService.removeById(id);
        ThrowUtils.throwIf(!result, ResCodeEnum.OPERATION_ERROR);
        return BaseResponseModel.success(true);

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
    public BaseResponseModel<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest) {
        // 1. 校验参数是否为空
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ResCodeEnum.PARAM_ERROR);
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
        // 1. 校验参数是否合法
        ThrowUtils.throwIf(id <= 0, ResCodeEnum.PARAM_ERROR);
        // 2. 查询数据库，判断图片是否存在
        PictureEntity picture = picturePictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ResCodeEnum.NOT_FOUND_ERROR);
        // 3. 转换为VO对象并返回
        return BaseResponseModel.success(picturePictureService.getPictureVO(picture, request));
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
    public BaseResponseModel<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {

        // 1. 获取分页参数：当前页码和每页大小
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 2. 防爬虫限制：单次查询不能超过20条
        ThrowUtils.throwIf(size > 20, ResCodeEnum.PARAM_ERROR);
        // 3. 构建查询条件并执行分页查询
        Page<PictureEntity> picturePage = picturePictureService.page(new Page<>(current, size),
                picturePictureService.getQueryWrapper(pictureQueryRequest));

        // 4. 转换为VO对象分页结果并返回
        return BaseResponseModel.success(picturePictureService.getPictureVoPage(picturePage, request));
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

        // 1. 校验参数是否合法
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ResCodeEnum.PARAM_ERROR);
        }
        // 2. 将请求参数转换为实体类
        PictureEntity picture = new PictureEntity();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 2.1 注意：将tags集合转换为JSON字符串
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 3. 数据准备和校验
        // 3.1 设置编辑时间为当前时间
        picture.setEditTime(new Date());
        // 3.2 执行业务规则校验
        picturePictureService.validPicture(picture);

        // 3.3 获取当前登录用户信息
        UserEntity loginUser = userService.getLoginUser(request);

        // 4. 判断图片是否存在
        PictureEntity oldPicture = picturePictureService.getById(picture.getId());
        // 5. 权限校验：仅本人和管理员可以编辑
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ResCodeEnum.NOT_AUTH_ERROR, "仅本人和管理者有权限编辑");
        }
        // 6. 执行数据库更新操作
        boolean result = picturePictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ResCodeEnum.OPERATION_ERROR);
        return BaseResponseModel.success(true);
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


}
