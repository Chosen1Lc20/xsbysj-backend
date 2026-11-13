package org.lc.xsbysjbackend.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.lc.xsbysjbackend.common.BaseResponse;
import org.lc.xsbysjbackend.common.DeleteRequest;
import org.lc.xsbysjbackend.common.ResultUtils;
import org.lc.xsbysjbackend.constant.PermissionConstant;
import org.lc.xsbysjbackend.exception.BusinessException;
import org.lc.xsbysjbackend.exception.ErrorCode;
import org.lc.xsbysjbackend.model.dto.defense.DefenseGroupAddRequest;
import org.lc.xsbysjbackend.model.dto.defense.DefenseGroupQueryRequest;
import org.lc.xsbysjbackend.model.dto.defense.DefenseGroupUpdateRequest;
import org.lc.xsbysjbackend.model.vo.DefenseGroupVO;
import org.lc.xsbysjbackend.service.DefenseGroupService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 答辩组接口。
 */
@RestController
@RequestMapping("/api/defense/group")
@RequiredArgsConstructor
public class DefenseGroupController {

    private final DefenseGroupService defenseGroupService;

    @SaCheckPermission(PermissionConstant.DEFENSE_ADD)
    @PostMapping("/add")
    public BaseResponse<Long> addDefenseGroup(
            @RequestBody DefenseGroupAddRequest defenseGroupAddRequest) {
        return ResultUtils.success(
                defenseGroupService.addDefenseGroup(defenseGroupAddRequest)
        );
    }

    @SaCheckPermission(PermissionConstant.DEFENSE_UPDATE)
    @PostMapping("/update")
    public BaseResponse<Boolean> updateDefenseGroup(
            @RequestBody DefenseGroupUpdateRequest defenseGroupUpdateRequest) {
        return ResultUtils.success(
                defenseGroupService.updateDefenseGroup(defenseGroupUpdateRequest)
        );
    }

    @SaCheckPermission(PermissionConstant.DEFENSE_DELETE)
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteDefenseGroup(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "答辩组主键不能为空");
        }
        return ResultUtils.success(
                defenseGroupService.deleteDefenseGroup(deleteRequest.getId())
        );
    }

    @SaCheckPermission(PermissionConstant.DEFENSE_READ)
    @GetMapping("/get/vo")
    public BaseResponse<DefenseGroupVO> getDefenseGroupVoById(@RequestParam long id) {
        return ResultUtils.success(defenseGroupService.getDefenseGroupVoById(id));
    }

    @SaCheckPermission(PermissionConstant.DEFENSE_READ)
    @PostMapping("/list/page/vo")
    public BaseResponse<IPage<DefenseGroupVO>> listDefenseGroupVoByPage(
            @RequestBody DefenseGroupQueryRequest defenseGroupQueryRequest) {
        return ResultUtils.success(
                defenseGroupService.listDefenseGroupVoByPage(defenseGroupQueryRequest)
        );
    }
}
