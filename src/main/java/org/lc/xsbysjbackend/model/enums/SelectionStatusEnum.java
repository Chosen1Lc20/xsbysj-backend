package org.lc.xsbysjbackend.model.enums;

import lombok.Getter;

/**
 * 选题状态。
 */
@Getter
public enum SelectionStatusEnum {

    PENDING("待审核"),

    APPROVED("审核通过"),

    REJECTED("审核拒绝"),

    CANCELED("已取消");

    private final String message;

    SelectionStatusEnum(String message) {
        this.message = message;
    }

    public static SelectionStatusEnum getByValue(String value) {
        if (value == null) {
            return null;
        }
        for (SelectionStatusEnum status : values()) {
            if (status.name().equals(value)) {
                return status;
            }
        }
        return null;
    }
}
