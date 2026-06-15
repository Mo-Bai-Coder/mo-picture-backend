package com.mobai.mopicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum UserRoleEnum {

    USER("用户", "user"),
    ADMIN("管理员", "admin");

    private final String roleName;
    private final String roleValue;

    UserRoleEnum(String roleName, String roleValue) {
        this.roleName = roleName;
        this.roleValue = roleValue;
    }

    public static UserRoleEnum getEnumByValue(String roleValue) {
        if (ObjUtil.isEmpty(roleValue)) {
            return null;
        }
        for (UserRoleEnum value : UserRoleEnum.values()) {
            if (value.roleValue.equals(roleValue)) {
                return value;
            }
        }
        return null;
    }
}
