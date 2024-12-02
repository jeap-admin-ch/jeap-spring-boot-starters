package ch.admin.bit.jeap.rest.tracing;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * Store the current username in a attribute, do this just at the end of the filter chain
 */
@Order(Ordered.LOWEST_PRECEDENCE - 10)
@Configuration
@ConditionalOnClass(JeapAuthenticationToken.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@RequiredArgsConstructor
class ServletStoreUserFilter extends OncePerRequestFilter {
    final static String USERNAME_ATTRIBUTE = ServletStoreUserFilter.class.getCanonicalName() + ".Username";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);
        Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication instanceof JeapAuthenticationToken)
                .map(authentication -> (JeapAuthenticationToken) authentication)
                .map(JeapAuthenticationToken::getTokenSubject)
                .ifPresent(user -> request.setAttribute(USERNAME_ATTRIBUTE, user));
    }
}
