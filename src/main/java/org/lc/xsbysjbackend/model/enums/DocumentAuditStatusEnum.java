package org.lc.xsbysjbackend.model.enums;

import lombok.Getter;

/**
 * 过程文档审核状态。
 */
@Getter
public enum DocumentAuditStatusEnum {

    PENDING("待审核"),

    APPROVED("审核通过"),

    REJECTED("审核驳回");

    private final String message;

    DocumentAuditStatusEnum(String message) {
        this.message = message;
    }

    public static DocumentAuditStatusEnum getByValue(String value) {
        if (value == null) {
            return null;
        }
        for (DocumentAuditStatusEnum status : values()) {
            if (status.name().equals(value)) {
                return status;
            }
        }
        return null;
    }
}
