package org.lc.xsbysjbackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 文件上传结果。
 */
@Data
@AllArgsConstructor
public class FileUploadVO implements Serializable {

    /**
     * 文件主键。
     */
    private Long id;

    /**
     * 文件类型：avatar、attachment。
     */
    private String fileType;

    /**
     * COS 对象键。
     */
    private String key;

    /**
     * 文件访问地址。
     */
    private String url;

    /**
     * 上传时的原始文件名。
     */
    private String fileName;

    /**
     * 文件扩展名。
     */
    private String fileExtension;

    /**
     * 文件媒体类型。
     */
    private String contentType;

    /**
     * 文件大小，单位为字节。
     */
    private Long fileSize;

    private static final long serialVersionUID = 1L;
}
