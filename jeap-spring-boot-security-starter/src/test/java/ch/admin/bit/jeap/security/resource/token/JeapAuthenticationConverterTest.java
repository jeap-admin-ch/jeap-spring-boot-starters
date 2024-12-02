package ch.admin.bit.jeap.security.resource.token;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JeapAuthenticationConverterTest {

    private static final String TOKEN_VALUE = "dummy_token_value";

    private static final String USER_ROLES_CLAIM = "userroles";
    private static final String BUSINESS_PARTNER_ROLES_CLAIM = "bproles";
    private static final String ADMIN_DIR_UID_CLAIM = "admin_dir_uid";

    private static final String USER_ROLE_1 = "user_role_1";
    private static final String USER_ROLE_2 = "user_role_2";
    private static final String USER_ROLE_3 = "user_role_3";

    private static final String BUSINESS_PARTNER_ROLE_1 = "business_partner_role_1";
    private static final String BUSINESS_PARTNER_ROLE_2 = "business_partner_role_2";
    private static final String BUSINESS_PARTNER_ROLE_3 = "business_partner_role_3";

    private static final String BUSINESS_PARTNER_1 = "business_partner_1";
    private static final String BUSINESS_PARTNER_2 = "business_partner_2";

    private static final String ADMIN_DIR_UID_1 = "U11111111";

    private JeapAuthenticationConverter converter;

    @BeforeEach
    void initialize() {
        converter = new JeapAuthenticationConverter(new DefaultAuthoritiesResolver());
    }

    @Test
    void testConvert_whenJwtContainsNoUserAndNoBusinesspartnerRoles_thenReturnsJeapAuthenticationTokenContainingNoRoles() {
        Jwt jwt = createJwtBuilder(TOKEN_VALUE).build();

        AbstractAuthenticationToken authenticationToken = converter.convert(jwt);

        assertThat(authenticationToken).isInstanceOf(JeapAuthenticationToken.class);
        JeapAuthenticationToken jeapAuthenticationToken = (JeapAuthenticationToken) authenticationToken;
        assertThat(jeapAuthenticationToken.getToken()).isEqualTo(jwt);
        assertThat(jeapAuthenticationToken.getUserRoles()).isEmpty();
        assertThat(jeapAuthenticationToken.getBusinessPartnerRoles()).isEmpty();
    }

    @Test
    void testConvert_whenJwtContainsEmptyUserAndEmptyBusinesspartnerRolesClaims_thenReturnsJeapAuthenticationTokenContainingNoRoles() {
        Jwt jwt = createJwtBuilder(TOKEN_VALUE).claim(USER_ROLES_CLAIM, Collections.emptyList()).claim(BUSINESS_PARTNER_ROLES_CLAIM, Collections.emptyMap()).build();

        AbstractAuthenticationToken authenticationToken = converter.convert(jwt);

        assertThat(authenticationToken).isInstanceOf(JeapAuthenticationToken.class);
        JeapAuthenticationToken jeapAuthenticationToken = (JeapAuthenticationToken) authenticationToken;
        assertThat(jeapAuthenticationToken.getToken()).isEqualTo(jwt);
        assertThat(jeapAuthenticationToken.getUserRoles()).isEmpty();
        assertThat(jeapAuthenticationToken.getBusinessPartnerRoles()).isEmpty();
    }

    @Test
    void testConvert_whenJwtContainsUserAndBusinesspartnerRoles_thenReturnsJeapAuthenticationTokenContainingThoseRoles() {
        Jwt jwt = createJwtBuilder(TOKEN_VALUE).
                claim(USER_ROLES_CLAIM, List.of(USER_ROLE_1, USER_ROLE_2, USER_ROLE_3)).
                claim(BUSINESS_PARTNER_ROLES_CLAIM, Map.of(
                        BUSINESS_PARTNER_1, List.of(BUSINESS_PARTNER_ROLE_1, BUSINESS_PARTNER_ROLE_2),
                        BUSINESS_PARTNER_2, List.of(BUSINESS_PARTNER_ROLE_3))).
                build();

        AbstractAuthenticationToken authenticationToken = converter.convert(jwt);

        assertThat(authenticationToken).isInstanceOf(JeapAuthenticationToken.class);
        JeapAuthenticationToken jeapAuthenticationToken = (JeapAuthenticationToken) authenticationToken;
        assertThat(jeapAuthenticationToken.getToken()).isEqualTo(jwt);
        assertThat(jeapAuthenticationToken.getUserRoles()).containsOnly(USER_ROLE_1, USER_ROLE_2, USER_ROLE_3);
        assertThat(jeapAuthenticationToken.getBusinessPartnerRoles()).containsOnlyKeys(BUSINESS_PARTNER_1, BUSINESS_PARTNER_2);
        assertThat(jeapAuthenticationToken.getBusinessPartnerRoles().get(BUSINESS_PARTNER_1)).containsOnly(BUSINESS_PARTNER_ROLE_1, BUSINESS_PARTNER_ROLE_2);
        assertThat(jeapAuthenticationToken.getBusinessPartnerRoles().get(BUSINESS_PARTNER_2)).containsOnly(BUSINESS_PARTNER_ROLE_3);
    }

    @Test
    void testConvert_whenJwtContainsAdminDirUIDClaim_thenReturnsJeapAuthenticationTokenContainingThatClaim() {
        Jwt jwt = createJwtBuilder(TOKEN_VALUE).
                claim(ADMIN_DIR_UID_CLAIM, ADMIN_DIR_UID_1).
                build();

        AbstractAuthenticationToken authenticationToken = converter.convert(jwt);

        assertThat(authenticationToken).isInstanceOf(JeapAuthenticationToken.class);
        JeapAuthenticationToken jeapAuthenticationToken = (JeapAuthenticationToken) authenticationToken;
        assertThat(jeapAuthenticationToken.getToken()).isEqualTo(jwt);
        assertThat(jeapAuthenticationToken.getAdminDirUID()).isEqualTo(ADMIN_DIR_UID_1);
    }

    @Test
    void testConvert_whenCustomAuthoritiesResolver_thenIgnoreDefault() {
        // given
        AuthoritiesResolver authoritiesResolverMock = mock(AuthoritiesResolver.class);
        GrantedAuthority authority1 = authority("role1");
        GrantedAuthority authority2 = authority("role2");
        when(authoritiesResolverMock.deriveAuthoritiesFromRoles(any(), any())).thenReturn(List.of(authority1, authority2));
        converter = new JeapAuthenticationConverter(authoritiesResolverMock);

        Jwt jwt = createJwtBuilder(TOKEN_VALUE).build();

        // when
        AbstractAuthenticationToken authenticationToken = converter.convert(jwt);

        // then
        assertThat(authenticationToken).isInstanceOf(JeapAuthenticationToken.class);
        JeapAuthenticationToken jeapAuthenticationToken = (JeapAuthenticationToken) authenticationToken;
        assertThat(jeapAuthenticationToken.getAuthorities()).containsExactlyInAnyOrder(authority1, authority2);
    }

    @Test
    void testConvert_whenNoCustomAuthoritiesResolver_thenUseDefault() {
        // given
        converter = new JeapAuthenticationConverter(new DefaultAuthoritiesResolver());

        Jwt jwt = createJwtBuilder(TOKEN_VALUE).
                claim(USER_ROLES_CLAIM, List.of(USER_ROLE_1, USER_ROLE_2)).
                claim(BUSINESS_PARTNER_ROLES_CLAIM, Map.of(
                        BUSINESS_PARTNER_1, List.of(BUSINESS_PARTNER_ROLE_1, BUSINESS_PARTNER_ROLE_2),
                        BUSINESS_PARTNER_2, List.of(BUSINESS_PARTNER_ROLE_3))).
                build();

        // when
        AbstractAuthenticationToken authenticationToken = converter.convert(jwt);

        // then
        assertThat(authenticationToken).isInstanceOf(JeapAuthenticationToken.class);
        JeapAuthenticationToken jeapAuthenticationToken = (JeapAuthenticationToken) authenticationToken;
        assertThat(jeapAuthenticationToken.getAuthorities()).containsExactlyInAnyOrder(authority("ROLE_user_role_1"),
                authority("ROLE_user_role_2"), authority("ROLE_business_partner_role_1"),
                authority("ROLE_business_partner_role_2"), authority("ROLE_business_partner_role_3")
        );
    }

    private GrantedAuthority authority(String authority) {
        return new SimpleGrantedAuthority(authority);
    }

    private Jwt.Builder createJwtBuilder(String tokenValue) {
        // The token value, at least one header and at least one claim is required
        return Jwt.withTokenValue(tokenValue).
                header("dummy_header", "dummy_header_value").
                claim("dummy_claim", "dummy_claim_value");
    }
}
