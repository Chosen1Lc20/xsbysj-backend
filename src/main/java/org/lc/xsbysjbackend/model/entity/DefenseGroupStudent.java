package org.lc.xsbysjbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 答辩组学生关联。
 */
@Data
@TableName("defense_group_student")
public class DefenseGroupStudent {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long groupid;

    private Long studentid;

    private Long selectionid;

    private Date createtime;

    private Date edittime;

    private Date updatetime;
}
