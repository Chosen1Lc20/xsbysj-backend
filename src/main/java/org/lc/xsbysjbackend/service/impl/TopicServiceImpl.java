package org.lc.xsbysjbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lc.xsbysjbackend.model.entity.Topic;
import org.lc.xsbysjbackend.service.TopicService;
import org.lc.xsbysjbackend.mapper.TopicMapper;
import org.springframework.stereotype.Service;

/**
* @author lianchao0921
* @description 针对表【topic(课题表)】的数据库操作Service实现
* @createDate 2026-11-11 15:19:31
*/
@Service
public class TopicServiceImpl extends ServiceImpl<TopicMapper, Topic>
    implements TopicService{

}
