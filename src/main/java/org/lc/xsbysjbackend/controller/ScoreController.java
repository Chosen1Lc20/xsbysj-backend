package org.lc.xsbysjbackend.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.lc.xsbysjbackend.common.BaseResponse;
import org.lc.xsbysjbackend.common.ResultUtils;
import org.lc.xsbysjbackend.constant.PermissionConstant;
import org.lc.xsbysjbackend.model.dto.score.ScoreQueryRequest;
import org.lc.xsbysjbackend.model.dto.score.ScoreSaveRequest;
import org.lc.xsbysjbackend.model.vo.ScoreVO;
import org.lc.xsbysjbackend.service.ScoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 成绩接口。
 */
@RestController
@RequestMapping("/api/score")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreService scoreService;

    @SaCheckPermission(PermissionConstant.SCORE_SAVE)
    @PostMapping("/save")
    public BaseResponse<ScoreVO> saveScore(@RequestBody ScoreSaveRequest scoreSaveRequest) {
        return ResultUtils.success(scoreService.saveScore(scoreSaveRequest));
    }

    @SaCheckPermission(PermissionConstant.SCORE_READ)
    @GetMapping("/get/vo")
    public BaseResponse<ScoreVO> getScoreVoById(@RequestParam long id) {
        return ResultUtils.success(scoreService.getScoreVoById(id));
    }

    @SaCheckPermission(PermissionConstant.SCORE_READ)
    @GetMapping("/my")
    public BaseResponse<ScoreVO> getMyScore() {
        return ResultUtils.success(scoreService.getMyScore());
    }

    @SaCheckPermission(PermissionConstant.SCORE_READ)
    @PostMapping("/list/page/vo")
    public BaseResponse<IPage<ScoreVO>> listScoreVoByPage(
            @RequestBody ScoreQueryRequest scoreQueryRequest) {
        return ResultUtils.success(scoreService.listScoreVoByPage(scoreQueryRequest));
    }
}
