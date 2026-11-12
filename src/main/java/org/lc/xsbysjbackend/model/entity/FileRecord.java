package org.lc.xsbysjbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 文件记录表。
 */
@Data
@TableName("file_record")
public class FileRecord {

    /**
     * 文件主键。
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 上传用户主键。
     */
    @TableField("uploadUserId")
    private Long uploadUserId;

    /**
     * COS 对象键。
     */
    @TableField("fileKey")
    private String fileKey;

    /**
     * 文件访问地址。
     */
    @TableField("fileUrl")
    private String fileUrl;

    /**
     * 文件类型：avatar、attachment。
     */
    @TableField("fileType")
    private String fileType;

    /**
     * 文件扩展名。
     */
    @TableField("fileExtension")
    private String fileExtension;

    /**
     * 原始文件名。
     */
    @TableField("fileName")
    private String fileName;

    /**
     * 文件大小，单位为字节。
     */
    @TableField("fileSize")
    private Long fileSize;

    /**
     * 文件媒体类型。
     */
    @TableField("contentType")
    private String contentType;

    /**
     * 创建时间。
     */
    @TableField("createTime")
    private Date createTime;

    /**
     * 编辑时间。
     */
    @TableField("EditTime")
    private Date editTime;

    /**
     * 更新时间。
     */
    @TableField("UpdateTime")
    private Date updateTime;
}
