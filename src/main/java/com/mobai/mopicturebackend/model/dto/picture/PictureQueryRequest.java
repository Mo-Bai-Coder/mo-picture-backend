package com.mobai.mopicturebackend.model.dto.picture;

import com.mobai.mopicturebackend.common.PageRequestInfo;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 图片查询请求DTO
 * 用于接收前端传来的图片查询/搜索条件，支持分页查询
 */
@Data
public class PictureQueryRequest extends PageRequestInfo {

    /**
     * 图片ID
     */
    private Long id;

    /**
     * 图片名称（支持模糊查询）
     */
    private String name;

    /**
     * 图片简介/描述（支持模糊查询）
     */
    private String introduction;

    /**
     * 图片分类
     */
    private String category;

    /**
     * 图片标签列表（支持多标签筛选）
     */
    private List<String> tags;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 搜索词（同时搜名称、简介等）
     */
    private String searchText;

    /**
     * 用户ID，用于查询指定用户上传的图片
     */
    private Long userId;

    /**
     * 状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 审核人 id
     */
    private Long reviewerId;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 是否只查询 spaceId 为 null 的数据
     */
    private boolean nullSpaceId;

    /*
     * 开始编辑时间
     */
    private Date startEditTime;

    /*
     * 结束编辑时间
     */
    private Date endEditTime;
}
