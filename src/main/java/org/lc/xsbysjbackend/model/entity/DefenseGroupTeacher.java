package org.lc.xsbysjbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 答辩组教师关联。
 */
@Data
@TableName("defense_group_teacher")
public class DefenseGroupTeacher {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long groupid;

    private Long teacherid;

    private Date createtime;

    private Date edittime;

    private Date updatetime;
}
