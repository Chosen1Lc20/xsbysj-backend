package org.lc.xsbysjbackend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.lc.xsbysjbackend.model.dto.score.ScoreQueryRequest;
import org.lc.xsbysjbackend.model.dto.score.ScoreSaveRequest;
import org.lc.xsbysjbackend.model.entity.Score;
import org.lc.xsbysjbackend.model.vo.ScoreVO;

/**
* @author lianchao0921
* @description 针对表【score(成绩表)】的数据库操作Service
* @createDate 2026-11-11 15:19:31
*/
public interface ScoreService extends IService<Score> {

    /**
     * 指导教师新增或更新本人课题学生成绩。
     */
    ScoreVO saveScore(ScoreSaveRequest scoreSaveRequest);

    /**
     * 根据当前用户角色查询成绩详情。
     */
    ScoreVO getScoreVoById(long id);

    /**
     * 查询当前学生的成绩。
     */
    ScoreVO getMyScore();

    /**
     * 根据当前用户角色分页查询成绩。
     */
    IPage<ScoreVO> listScoreVoByPage(ScoreQueryRequest scoreQueryRequest);
}
