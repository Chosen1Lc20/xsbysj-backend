package org.lc.xsbysjbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lc.xsbysjbackend.model.entity.Defense;
import org.lc.xsbysjbackend.service.DefenseService;
import org.lc.xsbysjbackend.mapper.DefenseMapper;
import org.springframework.stereotype.Service;

/**
* @author lianchao0921
* @description 针对表【defense(答辩分组表)】的数据库操作Service实现
* @createDate 2026-11-11 15:19:31
*/
@Service
public class DefenseServiceImpl extends ServiceImpl<DefenseMapper, Defense>
    implements DefenseService{

}
