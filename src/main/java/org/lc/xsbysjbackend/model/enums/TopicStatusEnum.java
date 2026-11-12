package org.lc.xsbysjbackend.model.enums;

import lombok.Getter;

/**
 * 课题状态。
 */
@Getter
public enum TopicStatusEnum {

    DRAFT("草稿"),

    PENDING("待审核"),

    APPROVED("审核通过"),

    REJECTED("审核拒绝");

    private final String message;

    TopicStatusEnum(String message) {
        this.message = message;
    }

    public static TopicStatusEnum getByValue(String value) {
        if (value == null) {
            return null;
        }
        for (TopicStatusEnum status : values()) {
            if (status.name().equals(value)) {
                return status;
            }
        }
        return null;
    }
}
