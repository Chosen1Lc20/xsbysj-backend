package org.lc.xsbysjbackend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.lc.xsbysjbackend.model.dto.defense.DefenseGroupAddRequest;
import org.lc.xsbysjbackend.model.dto.defense.DefenseGroupQueryRequest;
import org.lc.xsbysjbackend.model.dto.defense.DefenseGroupUpdateRequest;
import org.lc.xsbysjbackend.model.entity.DefenseGroup;
import org.lc.xsbysjbackend.model.vo.DefenseGroupVO;

/**
 * 答辩组服务。
 */
public interface DefenseGroupService extends IService<DefenseGroup> {

    /**
     * 管理员创建答辩组并分配教师和学生。
     */
    long addDefenseGroup(DefenseGroupAddRequest defenseGroupAddRequest);

    /**
     * 管理员编辑答辩组及成员。
     */
    boolean updateDefenseGroup(DefenseGroupUpdateRequest defenseGroupUpdateRequest);

    /**
     * 管理员删除答辩组及成员关联。
     */
    boolean deleteDefenseGroup(long id);

    /**
     * 根据当前用户角色查询答辩组详情。
     */
    DefenseGroupVO getDefenseGroupVoById(long id);

    /**
     * 根据当前用户角色分页查询答辩组。
     */
    IPage<DefenseGroupVO> listDefenseGroupVoByPage(
            DefenseGroupQueryRequest defenseGroupQueryRequest);
}
