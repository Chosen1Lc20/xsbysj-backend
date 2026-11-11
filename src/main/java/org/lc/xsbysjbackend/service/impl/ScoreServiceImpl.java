package org.lc.xsbysjbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lc.xsbysjbackend.model.entity.Score;
import org.lc.xsbysjbackend.service.ScoreService;
import org.lc.xsbysjbackend.mapper.ScoreMapper;
import org.springframework.stereotype.Service;

/**
* @author lianchao0921
* @description 针对表【score(成绩表)】的数据库操作Service实现
* @createDate 2026-11-11 15:19:31
*/
@Service
public class ScoreServiceImpl extends ServiceImpl<ScoreMapper, Score>
    implements ScoreService{

}
