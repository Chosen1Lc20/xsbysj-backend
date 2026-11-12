package org.lc.xsbysjbackend.model.enums;

import lombok.Getter;

/**
 * 文件类型。
 */
@Getter
public enum FileTypeEnum {

    AVATAR("avatar"),

    ATTACHMENT("attachment");

    private final String value;

    FileTypeEnum(String value) {
        this.value = value;
    }
}
