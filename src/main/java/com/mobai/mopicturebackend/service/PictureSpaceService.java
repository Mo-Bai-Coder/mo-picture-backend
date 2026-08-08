package com.mobai.mopicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mobai.mopicturebackend.model.dto.space.SpaceAddRequest;
import com.mobai.mopicturebackend.model.dto.space.SpaceQueryRequest;
import com.mobai.mopicturebackend.model.entity.SpaceEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mobai.mopicturebackend.model.entity.UserEntity;
import com.mobai.mopicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author MoBai
* @description 针对表【picture_space(空间)】的数据库操作Service
* @createDate 2026-07-25 22:45:39
*/
public interface PictureSpaceService extends IService<SpaceEntity> {

    void validSpace(SpaceEntity space, boolean add);

    void fillSpaceBySpaceLevel(SpaceEntity space);

    /**
     * 创建空间
     *
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, UserEntity loginUser);

    /**
     * 获取查询对象
     *
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<SpaceEntity> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);


    /**
     * 获取空间包装类（分页）
     *
     * @param spacePage
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<SpaceEntity> spacePage, HttpServletRequest request);

    /**
     * 获取空间包装类（单条）
     *
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(SpaceEntity space, HttpServletRequest request);



}
