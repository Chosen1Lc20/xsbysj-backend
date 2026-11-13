package org.lc.xsbysjbackend.model.enums;

import lombok.Getter;

/**
 * 毕业设计过程文档类型。
 */
@Getter
public enum DocumentTypeEnum {

    OPENING_REPORT("开题报告"),

    MIDTERM_CHECK("中期检查"),

    FIRST_DRAFT("论文初稿"),

    FINAL_DRAFT("论文终稿");

    private final String message;

    DocumentTypeEnum(String message) {
        this.message = message;
    }

    public static DocumentTypeEnum getByValue(String value) {
        if (value == null) {
            return null;
        }
        for (DocumentTypeEnum documentType : values()) {
            if (documentType.name().equals(value)) {
                return documentType;
            }
        }
        return null;
    }
}
