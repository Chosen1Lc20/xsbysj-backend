package org.lc.xsbysjbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lc.xsbysjbackend.model.entity.Selection;
import org.lc.xsbysjbackend.service.SelectionService;
import org.lc.xsbysjbackend.mapper.SelectionMapper;
import org.springframework.stereotype.Service;

/**
* @author lianchao0921
* @description 针对表【selection(选题表)】的数据库操作Service实现
* @createDate 2026-11-11 15:19:31
*/
@Service
public class SelectionServiceImpl extends ServiceImpl<SelectionMapper, Selection>
    implements SelectionService{

}
