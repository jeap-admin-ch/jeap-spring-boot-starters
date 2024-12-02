package ch.admin.bit.jeap.security.it.resource;

import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TestRoles {

    public static final String SEMANTIC_AUTH_READ_ROLE = SemanticApplicationRole.builder()
            .system("jme")
            .resource("auth")
            .operation("read")
            .build().toString();

    public static final String SEMANTIC_OTHER_READ_ROLE = SemanticApplicationRole.builder()
            .system("jme")
            .resource("other")
            .operation("read")
            .build().toString();

    public static final String SEMANTIC_YET_ANOTHER_READ_ROLE = SemanticApplicationRole.builder()
            .system("jme")
            .resource("yetanother")
            .operation("read")
            .build().toString();

    public static final String SIMPLE_AUTH_READ_ROLE = "authentication:read";
    public static final String SIMPLE_OTHER_READ_ROLE = "other:read";

}
