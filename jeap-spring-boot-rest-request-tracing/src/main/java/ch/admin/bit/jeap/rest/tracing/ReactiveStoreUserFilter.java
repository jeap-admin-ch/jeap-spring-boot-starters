package ch.admin.bit.jeap.rest.tracing;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Store the current username in a attribute, do this just at the end of the filter chain
 */
@Order(Ordered.LOWEST_PRECEDENCE - 10)
@Configuration
@ConditionalOnClass(JeapAuthenticationToken.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
class ReactiveStoreUserFilter implements WebFilter {
    final static String USERNAME_ATTRIBUTE = ReactiveStoreUserFilter.class.getCanonicalName() + ".Username";

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        return chain.filter(exchange)
                .then(ReactiveSecurityContextHolder.getContext())
                .filter(ccontext -> ccontext.getAuthentication() != null)
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication instanceof JeapAuthenticationToken)
                .map(authentication -> (JeapAuthenticationToken) authentication)
                .filter(authentication -> authentication.getTokenSubject() != null)
                .map(JeapAuthenticationToken::getTokenSubject)
                .doOnNext(user -> exchange.getAttributes().put(USERNAME_ATTRIBUTE, user))
                .then();
    }
}
