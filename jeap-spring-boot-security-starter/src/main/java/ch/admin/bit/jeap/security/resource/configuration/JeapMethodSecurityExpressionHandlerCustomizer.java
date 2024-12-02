package ch.admin.bit.jeap.security.resource.configuration;

import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;

public interface JeapMethodSecurityExpressionHandlerCustomizer {

    /**
     * Customize the expression handler configuration further after the jEAP security pre-configuration has been applied.
     * You might e.g. call setPermissionEvaluator(), setTrustResolver(), setRoleHierarchy() or setDefaultRolePrefix() on
     * the expression handler. Or you might wrap the provided expression handler with your own implementation.
     *
     * @param expressionHandler Expression handler as pre-configured by jEAP security
     * @return A further customized expression handler
     */
    MethodSecurityExpressionHandler customize(DefaultMethodSecurityExpressionHandler expressionHandler);

}
