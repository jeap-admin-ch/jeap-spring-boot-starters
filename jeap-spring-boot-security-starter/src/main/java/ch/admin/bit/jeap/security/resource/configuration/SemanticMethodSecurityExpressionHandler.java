package ch.admin.bit.jeap.security.resource.configuration;

import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticRoleRepository;
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

import java.util.function.Supplier;

/**
 * An extension for the {@link DefaultMethodSecurityExpressionHandler} to allow the function of
 * {@link SemanticRoleRepository} to be used in the {@link PreAuthorize} and {@link PostAuthorize} annotations
 */
public class SemanticMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {
    private final String systemName;

    public SemanticMethodSecurityExpressionHandler(String systemName) {
        this.systemName = systemName;
    }

    // The reactive stack still seems to call the version providing the authentication and not an authentication supplier
    @Override
    protected MethodSecurityExpressionOperations createSecurityExpressionRoot(
            Authentication authentication, MethodInvocation invocation) {
        //Only use the extended functionality for our tokens, do default otherwise
        if (authentication instanceof JeapAuthenticationToken jeapAuthenticationToken) {
            SemanticMethodSecurityExpressionRoot root = new SemanticMethodSecurityExpressionRoot(jeapAuthenticationToken);
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
            SemanticMethodSecurityExpressionRoot root = new SemanticMethodSecurityExpressionRoot(jeapAuthenticationToken);
            root.setThis(mi.getThis());
            root.setPermissionEvaluator(getPermissionEvaluator());
            root.setTrustResolver(getTrustResolver());
            root.setRoleHierarchy(getRoleHierarchy());
            root.setDefaultRolePrefix(getDefaultRolePrefix());
            context.setRootObject(root);
        }
        return context;
    }

    private class SemanticMethodSecurityExpressionRoot extends SecurityExpressionRoot implements
            MethodSecurityExpressionOperations {
        /**
         * All methods from {@link SemanticRoleRepository} can be used directly on this class as they are
         * delegated to a new instance of {@link SemanticRoleRepository}
         */
        @Delegate
        private final SemanticRoleRepository semanticRoleRepository;

        @Getter
        @Setter
        private Object filterObject;
        @Getter
        @Setter
        private Object returnObject;
        private Object target;

        SemanticMethodSecurityExpressionRoot(JeapAuthenticationToken authentication) {
            super(authentication);
            semanticRoleRepository = new SemanticRoleRepository(systemName, authentication);
        }

        @Override
        public Object getThis() {
            return target;
        }

        void setThis(Object target) {
            this.target = target;
        }
    }
}
