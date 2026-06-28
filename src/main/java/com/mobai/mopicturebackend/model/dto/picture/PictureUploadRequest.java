package com.mobai.mopicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {
    private static final long serialVersionUID = -409420516821080911L;

    /**
     * 图片id (用于修改）
     */
    private Long id;
}
