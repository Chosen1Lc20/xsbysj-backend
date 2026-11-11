package org.lc.xsbysjbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lc.xsbysjbackend.model.entity.Report;
import org.lc.xsbysjbackend.service.ReportService;
import org.lc.xsbysjbackend.mapper.ReportMapper;
import org.springframework.stereotype.Service;

/**
* @author lianchao0921
* @description 针对表【report(文档记录表)】的数据库操作Service实现
* @createDate 2026-11-11 15:19:31
*/
@Service
public class ReportServiceImpl extends ServiceImpl<ReportMapper, Report>
    implements ReportService{

}
