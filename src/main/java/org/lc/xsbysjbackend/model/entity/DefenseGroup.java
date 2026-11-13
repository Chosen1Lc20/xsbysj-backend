package org.lc.xsbysjbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 答辩组。
 */
@Data
@TableName("defense_group")
public class DefenseGroup {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String groupname;

    private Date defensetime;

    private String location;

    private String remark;

    private Date createtime;

    private Date edittime;

    private Date updatetime;
}
