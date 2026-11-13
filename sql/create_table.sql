-- 学生毕业设计管理系统数据库结构
-- 目标数据库：xsbysj
-- 本脚本只创建 xsbysj 数据库及其缺失的数据表，不删除或修改其他数据库。

CREATE DATABASE IF NOT EXISTS `xsbysj`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE `xsbysj`;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '用户主键（雪花ID）',
    `useraccount` VARCHAR(64) NOT NULL COMMENT '账号',
    `userNo` VARCHAR(64) NOT NULL COMMENT '学号或工号',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `name` VARCHAR(64) NOT NULL COMMENT '姓名',
    `role` VARCHAR(32) NOT NULL COMMENT '角色',
    `department` VARCHAR(128) DEFAULT NULL COMMENT '院系',
    `avatarUrl` VARCHAR(512) DEFAULT NULL COMMENT '头像地址',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `EditTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
    `UpdateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_useraccount` (`useraccount`),
    UNIQUE KEY `uk_user_userno` (`userNo`),
    KEY `idx_user_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

SET @rename_user_department = (
    SELECT IF(
        SUM(COLUMN_NAME = 'dept') = 1
            AND SUM(COLUMN_NAME = 'department') = 0,
        'ALTER TABLE `user` RENAME COLUMN `dept` TO `department`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME IN ('dept', 'department')
);
PREPARE user_department_stmt FROM @rename_user_department;
EXECUTE user_department_stmt;
DEALLOCATE PREPARE user_department_stmt;

ALTER TABLE `user`
    MODIFY COLUMN `department` VARCHAR(128) DEFAULT NULL COMMENT '院系';

CREATE TABLE IF NOT EXISTS `file_record` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '文件主键（雪花ID）',
    `uploadUserId` BIGINT UNSIGNED NOT NULL COMMENT '上传用户主键',
    `fileKey` VARCHAR(512) NOT NULL COMMENT 'COS对象键',
    `fileUrl` VARCHAR(512) NOT NULL COMMENT '文件访问地址',
    `fileType` VARCHAR(32) NOT NULL COMMENT '文件类型：avatar、attachment',
    `fileExtension` VARCHAR(16) NOT NULL COMMENT '文件扩展名',
    `fileName` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `fileSize` BIGINT UNSIGNED NOT NULL COMMENT '文件大小（字节）',
    `contentType` VARCHAR(128) NOT NULL COMMENT '文件媒体类型',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `EditTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
    `UpdateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_file_record_file_key` (`fileKey`),
    KEY `idx_file_record_upload_user` (`uploadUserId`),
    KEY `idx_file_record_type` (`fileType`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件记录表';

CREATE TABLE IF NOT EXISTS `topic` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '课题主键（雪花ID）',
    `teacherId` BIGINT UNSIGNED NOT NULL COMMENT '教师用户主键',
    `title` VARCHAR(255) NOT NULL COMMENT '课题名称',
    `description` TEXT DEFAULT NULL COMMENT '课题简介',
    `type` VARCHAR(64) DEFAULT NULL COMMENT '课题类型',
    `maxStudents` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '可选人数',
    `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `EditTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
    `UpdateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_topic_teacher` (`teacherId`),
    KEY `idx_topic_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课题表';

SET @add_topic_reviewer_id = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `topic` ADD COLUMN `reviewerId` BIGINT UNSIGNED DEFAULT NULL COMMENT ''审核管理员主键'' AFTER `status`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'topic'
      AND COLUMN_NAME = 'reviewerId'
);
PREPARE topic_reviewer_stmt FROM @add_topic_reviewer_id;
EXECUTE topic_reviewer_stmt;
DEALLOCATE PREPARE topic_reviewer_stmt;

SET @add_topic_audit_comment = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `topic` ADD COLUMN `auditComment` VARCHAR(1000) DEFAULT NULL COMMENT ''审核意见'' AFTER `reviewerId`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'topic'
      AND COLUMN_NAME = 'auditComment'
);
PREPARE topic_audit_comment_stmt FROM @add_topic_audit_comment;
EXECUTE topic_audit_comment_stmt;
DEALLOCATE PREPARE topic_audit_comment_stmt;

SET @add_topic_review_time = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `topic` ADD COLUMN `reviewTime` DATETIME DEFAULT NULL COMMENT ''审核时间'' AFTER `auditComment`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'topic'
      AND COLUMN_NAME = 'reviewTime'
);
PREPARE topic_review_time_stmt FROM @add_topic_review_time;
EXECUTE topic_review_time_stmt;
DEALLOCATE PREPARE topic_review_time_stmt;

CREATE TABLE IF NOT EXISTS `selection` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '选题主键（雪花ID）',
    `studentId` BIGINT UNSIGNED NOT NULL COMMENT '学生用户主键',
    `topicId` BIGINT UNSIGNED NOT NULL COMMENT '课题主键',
    `selectionTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '选题时间',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `EditTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
    `UpdateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_selection_student_topic` (`studentId`, `topicId`),
    KEY `idx_selection_topic` (`topicId`),
    KEY `idx_selection_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='选题表';

SET @add_selection_reviewer_id = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `selection` ADD COLUMN `reviewerId` BIGINT UNSIGNED DEFAULT NULL COMMENT ''审核教师主键'' AFTER `status`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'selection'
      AND COLUMN_NAME = 'reviewerId'
);
PREPARE selection_reviewer_stmt FROM @add_selection_reviewer_id;
EXECUTE selection_reviewer_stmt;
DEALLOCATE PREPARE selection_reviewer_stmt;

SET @add_selection_audit_comment = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `selection` ADD COLUMN `auditComment` VARCHAR(1000) DEFAULT NULL COMMENT ''审核意见'' AFTER `reviewerId`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'selection'
      AND COLUMN_NAME = 'auditComment'
);
PREPARE selection_audit_comment_stmt FROM @add_selection_audit_comment;
EXECUTE selection_audit_comment_stmt;
DEALLOCATE PREPARE selection_audit_comment_stmt;

SET @add_selection_review_time = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `selection` ADD COLUMN `reviewTime` DATETIME DEFAULT NULL COMMENT ''审核时间'' AFTER `auditComment`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'selection'
      AND COLUMN_NAME = 'reviewTime'
);
PREPARE selection_review_time_stmt FROM @add_selection_review_time;
EXECUTE selection_review_time_stmt;
DEALLOCATE PREPARE selection_review_time_stmt;

CREATE TABLE IF NOT EXISTS `assignment` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '任务书主键（雪花ID）',
    `topicId` BIGINT UNSIGNED NOT NULL COMMENT '课题主键',
    `content` LONGTEXT DEFAULT NULL COMMENT '任务书内容',
    `attachment` VARCHAR(512) DEFAULT NULL COMMENT '附件路径',
    `issueTime` DATETIME DEFAULT NULL COMMENT '下发时间',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `EditTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
    `UpdateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_assignment_topic` (`topicId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务书表';

SET @add_assignment_selection_id = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `assignment` ADD COLUMN `selectionId` BIGINT UNSIGNED NOT NULL COMMENT ''选题记录主键'' AFTER `id`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'assignment'
      AND COLUMN_NAME = 'selectionId'
);
PREPARE assignment_selection_stmt FROM @add_assignment_selection_id;
EXECUTE assignment_selection_stmt;
DEALLOCATE PREPARE assignment_selection_stmt;

SET @add_assignment_teacher_id = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `assignment` ADD COLUMN `teacherId` BIGINT UNSIGNED NOT NULL COMMENT ''指导教师主键'' AFTER `selectionId`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'assignment'
      AND COLUMN_NAME = 'teacherId'
);
PREPARE assignment_teacher_stmt FROM @add_assignment_teacher_id;
EXECUTE assignment_teacher_stmt;
DEALLOCATE PREPARE assignment_teacher_stmt;

SET @add_assignment_title = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `assignment` ADD COLUMN `title` VARCHAR(255) NOT NULL COMMENT ''任务书标题'' AFTER `topicId`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'assignment'
      AND COLUMN_NAME = 'title'
);
PREPARE assignment_title_stmt FROM @add_assignment_title;
EXECUTE assignment_title_stmt;
DEALLOCATE PREPARE assignment_title_stmt;

SET @add_assignment_file_record_id = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `assignment` ADD COLUMN `fileRecordId` BIGINT UNSIGNED DEFAULT NULL COMMENT ''任务书附件文件记录主键'' AFTER `attachment`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'assignment'
      AND COLUMN_NAME = 'fileRecordId'
);
PREPARE assignment_file_stmt FROM @add_assignment_file_record_id;
EXECUTE assignment_file_stmt;
DEALLOCATE PREPARE assignment_file_stmt;

SET @add_assignment_deadline = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `assignment` ADD COLUMN `deadline` DATETIME DEFAULT NULL COMMENT ''任务截止时间'' AFTER `fileRecordId`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'assignment'
      AND COLUMN_NAME = 'deadline'
);
PREPARE assignment_deadline_stmt FROM @add_assignment_deadline;
EXECUTE assignment_deadline_stmt;
DEALLOCATE PREPARE assignment_deadline_stmt;

SET @add_assignment_status = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `assignment` ADD COLUMN `status` VARCHAR(32) NOT NULL DEFAULT ''DRAFT'' COMMENT ''任务书状态'' AFTER `deadline`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'assignment'
      AND COLUMN_NAME = 'status'
);
PREPARE assignment_status_stmt FROM @add_assignment_status;
EXECUTE assignment_status_stmt;
DEALLOCATE PREPARE assignment_status_stmt;

ALTER TABLE `assignment`
    MODIFY COLUMN `issueTime` DATETIME DEFAULT NULL COMMENT '下发时间';

SET @add_assignment_selection_unique = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `assignment` ADD UNIQUE KEY `uk_assignment_selection` (`selectionId`)',
        'SELECT 1'
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'assignment'
      AND INDEX_NAME = 'uk_assignment_selection'
);
PREPARE assignment_selection_unique_stmt FROM @add_assignment_selection_unique;
EXECUTE assignment_selection_unique_stmt;
DEALLOCATE PREPARE assignment_selection_unique_stmt;

SET @add_assignment_teacher_index = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `assignment` ADD KEY `idx_assignment_teacher` (`teacherId`)',
        'SELECT 1'
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'assignment'
      AND INDEX_NAME = 'idx_assignment_teacher'
);
PREPARE assignment_teacher_index_stmt FROM @add_assignment_teacher_index;
EXECUTE assignment_teacher_index_stmt;
DEALLOCATE PREPARE assignment_teacher_index_stmt;

SET @add_assignment_status_index = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `assignment` ADD KEY `idx_assignment_status` (`status`)',
        'SELECT 1'
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'assignment'
      AND INDEX_NAME = 'idx_assignment_status'
);
PREPARE assignment_status_index_stmt FROM @add_assignment_status_index;
EXECUTE assignment_status_index_stmt;
DEALLOCATE PREPARE assignment_status_index_stmt;

CREATE TABLE IF NOT EXISTS `report` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '文档记录主键（雪花ID）',
    `studentId` BIGINT UNSIGNED NOT NULL COMMENT '学生用户主键',
    `documentType` VARCHAR(64) NOT NULL COMMENT '文档类型',
    `filePath` VARCHAR(512) NOT NULL COMMENT '文件路径',
    `auditStatus` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '审核状态',
    `auditComment` TEXT DEFAULT NULL COMMENT '审核意见',
    `reviewTime` DATETIME DEFAULT NULL COMMENT '审核时间',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `EditTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
    `UpdateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_report_student` (`studentId`),
    KEY `idx_report_audit_status` (`auditStatus`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文档记录表';

SET @add_report_selection_id = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `report` ADD COLUMN `selectionId` BIGINT UNSIGNED NOT NULL COMMENT ''选题记录主键'' AFTER `id`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report'
      AND COLUMN_NAME = 'selectionId'
);
PREPARE report_selection_stmt FROM @add_report_selection_id;
EXECUTE report_selection_stmt;
DEALLOCATE PREPARE report_selection_stmt;

SET @add_report_topic_id = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `report` ADD COLUMN `topicId` BIGINT UNSIGNED NOT NULL COMMENT ''课题主键'' AFTER `selectionId`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report'
      AND COLUMN_NAME = 'topicId'
);
PREPARE report_topic_stmt FROM @add_report_topic_id;
EXECUTE report_topic_stmt;
DEALLOCATE PREPARE report_topic_stmt;

SET @add_report_teacher_id = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `report` ADD COLUMN `teacherId` BIGINT UNSIGNED NOT NULL COMMENT ''指导教师主键'' AFTER `topicId`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report'
      AND COLUMN_NAME = 'teacherId'
);
PREPARE report_teacher_stmt FROM @add_report_teacher_id;
EXECUTE report_teacher_stmt;
DEALLOCATE PREPARE report_teacher_stmt;

SET @add_report_file_record_id = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `report` ADD COLUMN `fileRecordId` BIGINT UNSIGNED NOT NULL COMMENT ''文件记录主键'' AFTER `filePath`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report'
      AND COLUMN_NAME = 'fileRecordId'
);
PREPARE report_file_stmt FROM @add_report_file_record_id;
EXECUTE report_file_stmt;
DEALLOCATE PREPARE report_file_stmt;

SET @add_report_version = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `report` ADD COLUMN `version` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT ''文档版本号'' AFTER `fileRecordId`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report'
      AND COLUMN_NAME = 'version'
);
PREPARE report_version_stmt FROM @add_report_version;
EXECUTE report_version_stmt;
DEALLOCATE PREPARE report_version_stmt;

SET @add_report_latest = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `report` ADD COLUMN `isLatest` TINYINT(1) NOT NULL DEFAULT 1 COMMENT ''是否为最新版本'' AFTER `version`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report'
      AND COLUMN_NAME = 'isLatest'
);
PREPARE report_latest_stmt FROM @add_report_latest;
EXECUTE report_latest_stmt;
DEALLOCATE PREPARE report_latest_stmt;

SET @add_report_submit_time = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `report` ADD COLUMN `submitTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''提交时间'' AFTER `isLatest`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report'
      AND COLUMN_NAME = 'submitTime'
);
PREPARE report_submit_time_stmt FROM @add_report_submit_time;
EXECUTE report_submit_time_stmt;
DEALLOCATE PREPARE report_submit_time_stmt;

SET @add_report_reviewer_id = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `report` ADD COLUMN `reviewerId` BIGINT UNSIGNED DEFAULT NULL COMMENT ''审核教师主键'' AFTER `auditStatus`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report'
      AND COLUMN_NAME = 'reviewerId'
);
PREPARE report_reviewer_stmt FROM @add_report_reviewer_id;
EXECUTE report_reviewer_stmt;
DEALLOCATE PREPARE report_reviewer_stmt;

SET @add_report_version_unique = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `report` ADD UNIQUE KEY `uk_report_selection_type_version` (`selectionId`, `documentType`, `version`)',
        'SELECT 1'
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report'
      AND INDEX_NAME = 'uk_report_selection_type_version'
);
PREPARE report_version_unique_stmt FROM @add_report_version_unique;
EXECUTE report_version_unique_stmt;
DEALLOCATE PREPARE report_version_unique_stmt;

SET @add_report_teacher_index = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `report` ADD KEY `idx_report_teacher` (`teacherId`)',
        'SELECT 1'
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report'
      AND INDEX_NAME = 'idx_report_teacher'
);
PREPARE report_teacher_index_stmt FROM @add_report_teacher_index;
EXECUTE report_teacher_index_stmt;
DEALLOCATE PREPARE report_teacher_index_stmt;

SET @add_report_latest_index = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `report` ADD KEY `idx_report_latest` (`isLatest`)',
        'SELECT 1'
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report'
      AND INDEX_NAME = 'idx_report_latest'
);
PREPARE report_latest_index_stmt FROM @add_report_latest_index;
EXECUTE report_latest_index_stmt;
DEALLOCATE PREPARE report_latest_index_stmt;

DROP TABLE IF EXISTS `defense`;

CREATE TABLE IF NOT EXISTS `defense_group` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '答辩组主键（雪花ID）',
    `groupName` VARCHAR(64) NOT NULL COMMENT '答辩组名称',
    `defenseTime` DATETIME NOT NULL COMMENT '答辩时间',
    `location` VARCHAR(255) NOT NULL COMMENT '答辩地点',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '答辩备注',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `EditTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
    `UpdateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_defense_group_name` (`groupName`),
    KEY `idx_defense_group_time` (`defenseTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='答辩组表';

CREATE TABLE IF NOT EXISTS `defense_group_teacher` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '答辩组教师关联主键（雪花ID）',
    `groupId` BIGINT UNSIGNED NOT NULL COMMENT '答辩组主键',
    `teacherId` BIGINT UNSIGNED NOT NULL COMMENT '答辩教师主键',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `EditTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
    `UpdateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_defense_group_teacher` (`groupId`, `teacherId`),
    KEY `idx_defense_teacher` (`teacherId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='答辩组教师关联表';

CREATE TABLE IF NOT EXISTS `defense_group_student` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '答辩组学生关联主键（雪花ID）',
    `groupId` BIGINT UNSIGNED NOT NULL COMMENT '答辩组主键',
    `studentId` BIGINT UNSIGNED NOT NULL COMMENT '答辩学生主键',
    `selectionId` BIGINT UNSIGNED NOT NULL COMMENT '选题记录主键',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `EditTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
    `UpdateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_defense_group_student` (`groupId`, `studentId`),
    UNIQUE KEY `uk_defense_student` (`studentId`),
    UNIQUE KEY `uk_defense_selection` (`selectionId`),
    KEY `idx_defense_group` (`groupId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='答辩组学生关联表';

CREATE TABLE IF NOT EXISTS `score` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '成绩主键（雪花ID）',
    `studentId` BIGINT UNSIGNED NOT NULL COMMENT '学生用户主键',
    `processScore` DECIMAL(4, 1) DEFAULT NULL COMMENT '过程成绩',
    `defenseScore` DECIMAL(4, 1) DEFAULT NULL COMMENT '答辩成绩',
    `totalScore` DECIMAL(4, 1) DEFAULT NULL COMMENT '总评成绩',
    `entryTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '录入时间',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `EditTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
    `UpdateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_score_student` (`studentId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成绩表';
