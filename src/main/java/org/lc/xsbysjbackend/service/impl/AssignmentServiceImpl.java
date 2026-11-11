package org.lc.xsbysjbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lc.xsbysjbackend.model.entity.Assignment;
import org.lc.xsbysjbackend.service.AssignmentService;
import org.lc.xsbysjbackend.mapper.AssignmentMapper;
import org.springframework.stereotype.Service;

/**
* @author lianchao0921
* @description 针对表【assignment(任务书表)】的数据库操作Service实现
* @createDate 2026-11-11 15:19:31
*/
@Service
public class AssignmentServiceImpl extends ServiceImpl<AssignmentMapper, Assignment>
    implements AssignmentService{

}
