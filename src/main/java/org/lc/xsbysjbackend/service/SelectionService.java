package org.lc.xsbysjbackend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.lc.xsbysjbackend.model.dto.selection.SelectionApplyRequest;
import org.lc.xsbysjbackend.model.dto.selection.SelectionQueryRequest;
import org.lc.xsbysjbackend.model.dto.selection.SelectionReviewRequest;
import org.lc.xsbysjbackend.model.entity.Selection;
import org.lc.xsbysjbackend.model.vo.SelectionVO;

/**
* @author lianchao0921
* @description 针对表【selection(选题表)】的数据库操作Service
* @createDate 2026-11-11 15:19:31
*/
public interface SelectionService extends IService<Selection> {

    /**
     * 学生申请选题。
     */
    long applySelection(SelectionApplyRequest selectionApplyRequest);

    /**
     * 学生取消待审核选题。
     */
    boolean cancelSelection(long id);

    /**
     * 教师审核本人课题下的选题。
     */
    boolean reviewSelection(SelectionReviewRequest selectionReviewRequest);

    /**
     * 查询选题详情。
     */
    SelectionVO getSelectionVoById(long id);

    /**
     * 根据当前用户角色分页查询选题。
     */
    IPage<SelectionVO> listSelectionVoByPage(SelectionQueryRequest selectionQueryRequest);
}
