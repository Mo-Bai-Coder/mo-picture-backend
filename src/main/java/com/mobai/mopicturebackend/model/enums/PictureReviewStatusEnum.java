package com.mobai.mopicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 图片审核状态枚举类
 */
@Getter
public enum PictureReviewStatusEnum {
    REVIEWING("待审核", 0),
    PASS("审核通过", 1),
    REJECT("审核拒绝", 2);

    private final String text;
    private final int value;

    /**
     * 枚举构造函数
     * @param text
     * @param value
     */
    PictureReviewStatusEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     * @param value
     * @return
     */
    public static PictureReviewStatusEnum getEnumByValue(int value) {
        //判空
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        //for 循环 取值
        for (PictureReviewStatusEnum pictureReviewStatusEnum : PictureReviewStatusEnum.values()) {
            if (pictureReviewStatusEnum.value == value) {
                return pictureReviewStatusEnum;
            }
        }
        return null;
    }

}
