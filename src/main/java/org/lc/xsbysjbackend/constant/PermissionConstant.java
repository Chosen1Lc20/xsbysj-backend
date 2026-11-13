package org.lc.xsbysjbackend.constant;

import java.util.Set;

/**
 * 系统权限码。
 */
public interface PermissionConstant {

    String USER_ADD = "user:add";

    String USER_DELETE = "user:delete";

    String USER_GET = "user:get";

    String USER_LIST = "user:list";

    String USER_TEACHER_LIST = "user:teacher:list";

    String TOPIC_ADD = "topic:add";

    String TOPIC_UPDATE = "topic:update";

    String TOPIC_DELETE = "topic:delete";

    String TOPIC_SUBMIT = "topic:submit";

    String TOPIC_REVIEW = "topic:review";

    String TOPIC_READ = "topic:read";

    String FILE_UPLOAD = "file:upload";

    String FILE_UPDATE = "file:update";

    String SELECTION_APPLY = "selection:apply";

    String SELECTION_CANCEL = "selection:cancel";

    String SELECTION_REVIEW = "selection:review";

    String SELECTION_READ = "selection:read";

    String ASSIGNMENT_ADD = "assignment:add";

    String ASSIGNMENT_UPDATE = "assignment:update";

    String ASSIGNMENT_DELETE = "assignment:delete";

    String ASSIGNMENT_ISSUE = "assignment:issue";

    String ASSIGNMENT_READ = "assignment:read";

    String DOCUMENT_SUBMIT = "document:submit";

    String DOCUMENT_REVIEW = "document:review";

    String DOCUMENT_READ = "document:read";

    String DEFENSE_ADD = "defense:add";

    String DEFENSE_UPDATE = "defense:update";

    String DEFENSE_DELETE = "defense:delete";

    String DEFENSE_READ = "defense:read";

    Set<String> ALL_PERMISSIONS = Set.of(
            USER_ADD,
            USER_DELETE,
            USER_GET,
            USER_LIST,
            USER_TEACHER_LIST,
            TOPIC_ADD,
            TOPIC_UPDATE,
            TOPIC_DELETE,
            TOPIC_SUBMIT,
            TOPIC_REVIEW,
            TOPIC_READ,
            FILE_UPLOAD,
            FILE_UPDATE,
            SELECTION_APPLY,
            SELECTION_CANCEL,
            SELECTION_REVIEW,
            SELECTION_READ,
            ASSIGNMENT_ADD,
            ASSIGNMENT_UPDATE,
            ASSIGNMENT_DELETE,
            ASSIGNMENT_ISSUE,
            ASSIGNMENT_READ,
            DOCUMENT_SUBMIT,
            DOCUMENT_REVIEW,
            DOCUMENT_READ,
            DEFENSE_ADD,
            DEFENSE_UPDATE,
            DEFENSE_DELETE,
            DEFENSE_READ
    );
}
