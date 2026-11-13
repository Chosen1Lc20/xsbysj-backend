package org.lc.xsbysjbackend.model.dto.document;

import lombok.Data;

import java.io.Serializable;

/**
 * 学生提交过程文档请求。
 */
@Data
public class DocumentSubmitRequest implements Serializable {

    private Long selectionId;

    private String documentType;

    private Long fileRecordId;

    private static final long serialVersionUID = 1L;
}
