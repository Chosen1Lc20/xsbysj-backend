package org.lc.xsbysjbackend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.lc.xsbysjbackend.model.entity.Report;
import com.baomidou.mybatisplus.extension.service.IService;
import org.lc.xsbysjbackend.model.dto.document.DocumentQueryRequest;
import org.lc.xsbysjbackend.model.dto.document.DocumentReviewRequest;
import org.lc.xsbysjbackend.model.dto.document.DocumentSubmitRequest;
import org.lc.xsbysjbackend.model.vo.DocumentVO;

/**
* @author lianchao0921
* @description 针对表【report(文档记录表)】的数据库操作Service
* @createDate 2026-11-11 15:19:31
*/
public interface ReportService extends IService<Report> {

    /**
     * 学生提交过程文档。已驳回文档会以新版本重新提交。
     */
    long submitDocument(DocumentSubmitRequest documentSubmitRequest);

    /**
     * 教师审核本人课题学生的过程文档。
     */
    boolean reviewDocument(DocumentReviewRequest documentReviewRequest);

    /**
     * 根据当前用户角色查询过程文档详情。
     */
    DocumentVO getDocumentVoById(long id);

    /**
     * 根据当前用户角色分页查询过程文档。
     */
    IPage<DocumentVO> listDocumentVoByPage(DocumentQueryRequest documentQueryRequest);
}
