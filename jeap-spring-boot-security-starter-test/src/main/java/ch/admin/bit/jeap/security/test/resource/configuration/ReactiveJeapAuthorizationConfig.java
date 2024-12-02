package ch.admin.bit.jeap.security.test.resource.configuration;

import ch.admin.bit.jeap.security.resource.authentication.ReactiveSimpleAuthorization;
import ch.admin.bit.jeap.security.resource.configuration.JeapMethodSecurityExpressionHandlerCustomizer;
import ch.admin.bit.jeap.security.resource.configuration.SemanticMethodSecurityExpressionHandler;
import ch.admin.bit.jeap.security.resource.configuration.SimpleMethodSecurityExpressionHandler;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.ReactiveSemanticAuthorization;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
@EnableReactiveMethodSecurity
public class ReactiveJeapAuthorizationConfig {

    private final String systemName;
    private final ApplicationContext applicationContext;
    private final JeapMethodSecurityExpressionHandlerCustomizer expressionHandlerCustomizer;

    /**
     * Contructor to use in tests of applications authorizing against semantic roles and needing to customize the
     * method security expression handler created for this purpose by jEAP.
     *
     * @param systemName
     * @param applicationContext
     * @param expressionHandlerCustomizer
     */
    public ReactiveJeapAuthorizationConfig(String systemName, ApplicationContext applicationContext,
                                           JeapMethodSecurityExpressionHandlerCustomizer expressionHandlerCustomizer) {
        this.applicationContext = applicationContext;
        this.systemName = systemName;
        this.expressionHandlerCustomizer = expressionHandlerCustomizer;
    }

    /**
     * Contructor to use in tests of applications authorizing against semantic roles.
     *
     * @param systemName
     * @param applicationContext
     */
    public ReactiveJeapAuthorizationConfig(String systemName, ApplicationContext applicationContext) {
        this(systemName, applicationContext, null);
    }

    /**
     * Contructor to use in tests of applications that authorize against simple roles (i.e. not against semantic roles)
     * and that need to customize the method security expression handler created for this purpose by jEAP.
     *
     * @param applicationContext
     * @param expressionHandlerCustomizer
     */
    public ReactiveJeapAuthorizationConfig(ApplicationContext applicationContext,
                                           JeapMethodSecurityExpressionHandlerCustomizer expressionHandlerCustomizer) {
        this(null, applicationContext, expressionHandlerCustomizer);
    }

    /**
     * Contructor to use in tests of applications that authorize against simple roles (i.e. not against semantic roles).
     *
     * @param applicationContext*
     */
    public ReactiveJeapAuthorizationConfig(ApplicationContext applicationContext) {
        this(applicationContext, null);
    }

    // Overwrite {@link MethodSecurityExpressionHandler} to add jeap role authorization security expressions
    @Bean
    @Primary
    public MethodSecurityExpressionHandler customMethodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler expressionHandler;
        if (systemName != null) {
            // Support semantic role authorization in method security expressions
            expressionHandler = new SemanticMethodSecurityExpressionHandler(systemName);
        } else {
            // Support simple role authorization in method security expressions
            expressionHandler = new SimpleMethodSecurityExpressionHandler();
        }
        expressionHandler.setApplicationContext(applicationContext);
        return expressionHandlerCustomizer != null ? expressionHandlerCustomizer.customize(expressionHandler) : expressionHandler;
    }

    @Bean
    public ReactiveSimpleAuthorization simpleAuthorization() {
        return new ReactiveSimpleAuthorization();
    }

    @Bean
    public ReactiveSemanticAuthorization servletAuthorization() {
        return new ReactiveSemanticAuthorization(systemName);
    }

}
