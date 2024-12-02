package ch.admin.bit.jeap.security.resource.configuration;

import ch.admin.bit.jeap.security.resource.authentication.SimpleRoleRepository;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.util.Set;
import java.util.function.Supplier;

/**
 * An extension of {@link DefaultMethodSecurityExpressionHandler} to make the functions defined by
 * {@link SimpleRoleRepository} available in {@link PreAuthorize} and {@link PostAuthorize} annotations.
 */
public class SimpleMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {


    // The reactive stack still seems to call the version providing the authentication and not an authentication supplier
    @Override
    protected MethodSecurityExpressionOperations createSecurityExpressionRoot(Authentication authentication, MethodInvocation invocation) {
        // Only provide the additional functions for JEAP authentications
        if (authentication instanceof JeapAuthenticationToken jeapAuth) {
            SimpleMethodSecurityExpressionRoot root = new SimpleMethodSecurityExpressionRoot(jeapAuth);
            root.setThis(invocation.getThis());
            root.setPermissionEvaluator(getPermissionEvaluator());
            root.setTrustResolver(getTrustResolver());
            root.setRoleHierarchy(getRoleHierarchy());
            root.setDefaultRolePrefix(getDefaultRolePrefix());
            return root;
        } else {
            return super.createSecurityExpressionRoot(authentication, invocation);
        }
    }

    @Override
    public EvaluationContext createEvaluationContext(Supplier<Authentication> authentication, MethodInvocation mi) {
        StandardEvaluationContext context = (StandardEvaluationContext) super.createEvaluationContext(authentication, mi);
        //Only use the extended functionality for our tokens, do default otherwise
        if (authentication.get() instanceof JeapAuthenticationToken jeapAuthenticationToken) {
            SimpleMethodSecurityExpressionRoot root = new SimpleMethodSecurityExpressionRoot(jeapAuthenticationToken);
            root.setThis(mi.getThis());
            root.setPermissionEvaluator(getPermissionEvaluator());
            root.setTrustResolver(getTrustResolver());
            root.setRoleHierarchy(getRoleHierarchy());
            root.setDefaultRolePrefix(getDefaultRolePrefix());
            context.setRootObject(root);
        }
        return context;
    }


    private static class SimpleMethodSecurityExpressionRoot extends SecurityExpressionRoot implements MethodSecurityExpressionOperations {
        /**
         * All methods included in the interface SimpleAuthenticationExpressionRootMethods can be used directly on this
         * class as they are delegated to a new instance of {@link SimpleRoleRepository}.
         */
        @Delegate(types = SimpleAuthenticationExpressionRootMethods.class)
        private final SimpleRoleRepository simpleRoleRepository;

        @Getter
        @Setter
        private Object filterObject;

        @Getter
        @Setter
        private Object returnObject;

        private Object target;

        private SimpleMethodSecurityExpressionRoot(JeapAuthenticationToken jeapAuth) {
            super(jeapAuth);
            simpleRoleRepository = new SimpleRoleRepository(jeapAuth.getUserRoles(), jeapAuth.getBusinessPartnerRoles());
        }

        @Override
        public Object getThis() {
            return target;
        }

        private void setThis(Object target) {
            this.target = target;
        }
    }

    // Methods from SimpleRoleRepository to add to the custom security expression root.
    // hasRole() is not included as this method is already implemented by SecurityExpressionRoot and a collision would
    // occur if the method was included.
    private interface SimpleAuthenticationExpressionRootMethods {
        boolean hasRoleForPartner(String role, String businessPartner);
        boolean hasRoleForAllPartners(String role);
        Set<String> getPartnersForRole(String role);
    }

}
