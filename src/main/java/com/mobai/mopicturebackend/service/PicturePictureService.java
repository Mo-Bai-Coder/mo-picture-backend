package com.mobai.mopicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mobai.mopicturebackend.model.dto.picture.PictureQueryRequest;
import com.mobai.mopicturebackend.model.dto.picture.PictureReviewRequest;
import com.mobai.mopicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.mobai.mopicturebackend.model.dto.picture.PictureUploadRequest;
import com.mobai.mopicturebackend.model.entity.PictureEntity;
import com.mobai.mopicturebackend.model.entity.UserEntity;
import com.mobai.mopicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
 * @author MoBai
 * @description 针对表【picture_picture(图片)】的数据库操作Service
 * @createDate 2026-06-21 15:48:03
 */
public interface PicturePictureService extends IService<PictureEntity> {

    /**
     * 上传图片
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, UserEntity loginUser);

    /**
     * 用于将查询请求转为QueryWrapper
     */
    QueryWrapper<PictureEntity> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取图片包装类（单条）
     *
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(PictureEntity picture, HttpServletRequest request);

    /**
     * 获取图片包装类（多条）
     *
     * @param picturePage
     * @param request
     * @return
     */
    Page<PictureVO> getPictureVoPage(Page<PictureEntity> picturePage, HttpServletRequest request);

    /**
     * 校验图片
     * @param picture
     */
    void validPicture(PictureEntity picture);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, UserEntity loginUser);

    /**
     * 填充审核参数
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(PictureEntity picture, UserEntity loginUser) ;


    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest ,UserEntity loginUser);

}