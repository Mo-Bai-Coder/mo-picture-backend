package com.mobai.mopicturebackend.common;

import lombok.Data;
/**
 * 分页请求参数
 */
@Data
public class PageRequestInfo {

    /**
     * 当前页
     */
    private int pageNum;

    /***
     * 每页的数量
     */
    private int pageSize;

    /**
     * 当前页的数量
     */
    private int size;

    /**
     * 总页数
     */
    private int pages;

     /**
     * 排序字段
     */
    private String sorField;

     /**
     * 排序顺序（默认降序）
     */
    private String sortOrder = "descend";
}
