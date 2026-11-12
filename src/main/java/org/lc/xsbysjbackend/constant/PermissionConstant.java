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

    String TOPIC_ADD = "topic:add";

    String TOPIC_UPDATE = "topic:update";

    String TOPIC_DELETE = "topic:delete";

    String TOPIC_SUBMIT = "topic:submit";

    String TOPIC_REVIEW = "topic:review";

    String TOPIC_READ = "topic:read";

    String FILE_UPLOAD = "file:upload";

    String FILE_UPDATE = "file:update";

    Set<String> ALL_PERMISSIONS = Set.of(
            USER_ADD,
            USER_DELETE,
            USER_GET,
            USER_LIST,
            TOPIC_ADD,
            TOPIC_UPDATE,
            TOPIC_DELETE,
            TOPIC_SUBMIT,
            TOPIC_REVIEW,
            TOPIC_READ,
            FILE_UPLOAD,
            FILE_UPDATE
    );
}
