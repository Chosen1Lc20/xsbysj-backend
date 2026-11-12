package org.lc.xsbysjbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.lc.xsbysjbackend.model.entity.FileRecord;
import org.lc.xsbysjbackend.model.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

/**
* @author lianchao0921
* @description 针对表【file_record(文件记录表)】的数据库操作Service
* @createDate 2026-11-12 10:03:50
*/
public interface FileRecordService extends IService<FileRecord> {

    /**
     * 上传文件并保存文件记录。
     *
     * @param file         上传文件
     * @param uploadUserId 上传用户主键
     * @return 文件上传结果
     */
    FileUploadVO uploadFile(MultipartFile file, long uploadUserId);

    /**
     * 编辑文件。传入新文件时替换文件，否则仅修改文件名。
     *
     * @param id           文件主键
     * @param file         新文件，可为空
     * @param fileName     新文件名，可为空
     * @param uploadUserId 当前用户主键
     * @return 更新后的文件信息
     */
    FileUploadVO updateFile(Long id, MultipartFile file, String fileName, long uploadUserId);
}
