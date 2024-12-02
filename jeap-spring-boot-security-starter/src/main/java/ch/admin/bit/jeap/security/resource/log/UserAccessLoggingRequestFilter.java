package ch.admin.bit.jeap.security.resource.log;

import ch.admin.bit.jeap.security.resource.configuration.MvcSecurityConfiguration;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * The {@link UserAccessLoggingRequestFilter} bean is instantiated in the {@link MvcSecurityConfiguration} when the
 * {@code jeap.security.oauth2.resourceserver.log-user-access} property is set to {@code true}.
 * It logs the  request URL together with the currently authenticated user's name and extId (if it is a
 * {@link JeapAuthenticationToken}), else just the name of the principal.
 */
@Slf4j
public class UserAccessLoggingRequestFilter extends OncePerRequestFilter {

    private static final Set<String> BLACKLISTED_PATHS = Set.of("/actuator", "/api-docs", "/swagger-ui", "/ui/configuration");

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain) throws IOException, ServletException {
        final String url = Optional.ofNullable(request.getQueryString())
                .map(request.getRequestURL().append('?')::append)
                .orElseGet(request::getRequestURL)
                .toString();
        if (request.getUserPrincipal() instanceof JeapAuthenticationToken) {
            final JeapAuthenticationToken principal = (JeapAuthenticationToken) request.getUserPrincipal();
            log.info("{} ({}) authenticated for {} {}",
                    StructuredArguments.keyValue("username", principal.getTokenName()),
                    StructuredArguments.keyValue("extId", principal.getToken().getClaimAsString("ext_id")),
                    request.getMethod(),
                    url);
        } else log.info("{} authenticated for {} {}",
                StructuredArguments.keyValue("username", request.getUserPrincipal().getName()),
                request.getMethod(),
                url);
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return BLACKLISTED_PATHS.stream().anyMatch(request.getRequestURI()::contains) || request.getUserPrincipal() == null;
    }

}
