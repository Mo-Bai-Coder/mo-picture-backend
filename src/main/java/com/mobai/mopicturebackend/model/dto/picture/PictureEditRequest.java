package com.mobai.mopicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 图片编辑请求DTO
 * 用于接收前端传来的图片编辑信息
 */
@Data
public class PictureEditRequest implements Serializable {

    private static final long serialVersionUID = 2090060742724455155L;

    /**
     * 图片ID,用于标识要更新的图片
     */
    private Long id;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 图片简介/描述
     */
    private String introduction;

    /**
     * 图片分类
     */
    private String category;

    /**
     * 图片标签列表
     */
    private List<String> tags;


}
