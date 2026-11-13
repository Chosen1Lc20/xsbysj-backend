package org.lc.xsbysjbackend.service;

import org.junit.jupiter.api.Test;
import org.lc.xsbysjbackend.model.entity.Assignment;
import org.lc.xsbysjbackend.model.enums.AssignmentStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class AssignmentPersistenceTest {

    @Autowired
    private AssignmentService assignmentService;

    @Test
    void assignmentPersistsSelectionTeacherFileAndTaskFields() {
        Date deadline = new Date();
        Assignment assignment = new Assignment();
        assignment.setSelectionid(990000000000030001L);
        assignment.setTeacherid(990000000000010002L);
        assignment.setTopicid(990000000000020001L);
        assignment.setTitle("毕业设计任务书测试");
        assignment.setContent("完成系统设计、开发、测试和论文撰写。");
        assignment.setFilerecordid(990000000000040001L);
        assignment.setDeadline(deadline);
        assignment.setStatus(AssignmentStatusEnum.DRAFT.name());

        assignmentService.save(assignment);
        Assignment saved = assignmentService.getById(assignment.getId());

        assertNotNull(saved);
        assertEquals(assignment.getSelectionid(), saved.getSelectionid());
        assertEquals(assignment.getTeacherid(), saved.getTeacherid());
        assertEquals(assignment.getFilerecordid(), saved.getFilerecordid());
        assertEquals("毕业设计任务书测试", saved.getTitle());
        assertEquals(AssignmentStatusEnum.DRAFT.name(), saved.getStatus());
    }
}
