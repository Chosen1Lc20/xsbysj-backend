package org.lc.xsbysjbackend.model.enums;

import lombok.Getter;

/**
 * 任务书状态。
 */
@Getter
public enum AssignmentStatusEnum {

    DRAFT("草稿"),

    ISSUED("已下发");

    private final String message;

    AssignmentStatusEnum(String message) {
        this.message = message;
    }

    public static AssignmentStatusEnum getByValue(String value) {
        if (value == null) {
            return null;
        }
        for (AssignmentStatusEnum status : values()) {
            if (status.name().equals(value)) {
                return status;
            }
        }
        return null;
    }
}
