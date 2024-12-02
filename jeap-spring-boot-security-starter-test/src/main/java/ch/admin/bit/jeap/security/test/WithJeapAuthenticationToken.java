package ch.admin.bit.jeap.security.test;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.JeapAuthenticationTestTokenBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * For usage examples refer to the {@code README.md} in this project.
 */
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithJeapAuthenticationToken.WithJeapAuthenticationTokenContextFactory.class)
public @interface WithJeapAuthenticationToken {

    /**
     * Specify bp roles that the authenticated user has in the following format:
     * <ul>
     *     <li><code>"myBpIp=myBpRole1"</code></li>
     *     <li><code>"myBpId=myBpRole1,myBpRole2"</code></li>
     *     <li><code>"myBpId = myBpRole1, myBpRole2"</code></li>
     *     <li><code>"myBpId1=myBpRole1", "myBpId2=myBpRole2"</code></li>
     *     <li><code>"myBpId1 = myBpRole1", "myBpId2 = myBpRole2"</code></li>
     * </ul>
     */
    String[] bpRoles() default {};

    String displayName() default "displayName";

    String extId() default "extId";

    String familyName() default "familyName";

    String givenName() default "givenName";

    String username() default "username";

    String[] userRoles() default {};

    @Slf4j
    class WithJeapAuthenticationTokenContextFactory implements WithSecurityContextFactory<WithJeapAuthenticationToken> {

        @Override
        public SecurityContext createSecurityContext(WithJeapAuthenticationToken withVendorAuthentication) {
            var authenticationBuilder = JeapAuthenticationTestTokenBuilder.create()
                    .withClaim("name", withVendorAuthentication.displayName())
                    .withClaim("ext_id", withVendorAuthentication.extId())
                    .withFamilyName(withVendorAuthentication.familyName())
                    .withGivenName(withVendorAuthentication.givenName())
                    .withPreferredUsername(withVendorAuthentication.username())
                    .withUserRoles(withVendorAuthentication.userRoles());
            for (Map.Entry<String, String[]> bpRoles : getBpRoles(withVendorAuthentication.bpRoles()).entrySet())
                authenticationBuilder = authenticationBuilder.withBusinessPartnerRoles(bpRoles.getKey(), bpRoles.getValue());

            final JeapAuthenticationToken authentication = authenticationBuilder.build();
            final SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            log.debug("Set authentication in SecurityContextHolder: {}", authentication);
            return context;
        }

        protected Map<String, String[]> getBpRoles(final String[] allBpRoles) {
            final HashMap<String, String[]> bpRolesMap = new HashMap<>();
            if (allBpRoles != null && allBpRoles.length > 0)
                Arrays.stream(allBpRoles)
                        .filter(bpRoles -> bpRoles != null && !bpRoles.isBlank() && bpRoles.contains("="))
                        .map(bpRoles -> bpRoles.replace(" ", "").split("="))
                        .filter(split -> split.length <= 2)
                        .forEach(split -> bpRolesMap.put(split[0], split.length == 1 ? new String[]{} : split[1].split(",")));
            return bpRolesMap;
        }

    }

}
