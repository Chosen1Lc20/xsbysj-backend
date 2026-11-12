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

CREATE TABLE IF NOT EXISTS `assignment` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '任务书主键（雪花ID）',
    `topicId` BIGINT UNSIGNED NOT NULL COMMENT '课题主键',
    `content` LONGTEXT DEFAULT NULL COMMENT '任务书内容',
    `attachment` VARCHAR(512) DEFAULT NULL COMMENT '附件路径',
    `issueTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下发时间',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `EditTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
    `UpdateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_assignment_topic` (`topicId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务书表';

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

CREATE TABLE IF NOT EXISTS `defense` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '答辩分组主键（雪花ID）',
    `groupName` VARCHAR(64) NOT NULL COMMENT '组名',
    `studentId` BIGINT UNSIGNED NOT NULL COMMENT '学生用户主键',
    `teacherId` BIGINT UNSIGNED NOT NULL COMMENT '教师用户主键',
    `defenseTime` DATETIME NOT NULL COMMENT '答辩时间',
    `location` VARCHAR(255) NOT NULL COMMENT '答辩地点',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `EditTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
    `UpdateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_defense_group` (`groupName`),
    KEY `idx_defense_student` (`studentId`),
    KEY `idx_defense_teacher` (`teacherId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='答辩分组表';

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
