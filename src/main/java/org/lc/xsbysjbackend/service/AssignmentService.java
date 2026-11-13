package org.lc.xsbysjbackend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.lc.xsbysjbackend.model.entity.Assignment;
import com.baomidou.mybatisplus.extension.service.IService;
import org.lc.xsbysjbackend.model.dto.assignment.AssignmentAddRequest;
import org.lc.xsbysjbackend.model.dto.assignment.AssignmentQueryRequest;
import org.lc.xsbysjbackend.model.dto.assignment.AssignmentUpdateRequest;
import org.lc.xsbysjbackend.model.vo.AssignmentVO;

/**
* @author lianchao0921
* @description 针对表【assignment(任务书表)】的数据库操作Service
* @createDate 2026-11-11 15:19:31
*/
public interface AssignmentService extends IService<Assignment> {

    /**
     * 教师为本人课题下已通过的选题创建任务书草稿。
     */
    long addAssignment(AssignmentAddRequest assignmentAddRequest);

    /**
     * 教师编辑本人尚未下发的任务书。
     */
    boolean updateAssignment(AssignmentUpdateRequest assignmentUpdateRequest);

    /**
     * 教师删除本人尚未下发的任务书。
     */
    boolean deleteAssignment(long id);

    /**
     * 教师下发任务书。
     */
    boolean issueAssignment(long id);

    /**
     * 根据当前用户角色查询任务书详情。
     */
    AssignmentVO getAssignmentVoById(long id);

    /**
     * 根据当前用户角色分页查询任务书。
     */
    IPage<AssignmentVO> listAssignmentVoByPage(AssignmentQueryRequest assignmentQueryRequest);
}
