package ch.admin.bit.jeap.security.test.resource.configuration;

import ch.admin.bit.jeap.security.resource.authentication.ServletSimpleAuthorization;
import ch.admin.bit.jeap.security.resource.configuration.JeapMethodSecurityExpressionHandlerCustomizer;
import ch.admin.bit.jeap.security.resource.configuration.SemanticMethodSecurityExpressionHandler;
import ch.admin.bit.jeap.security.resource.configuration.SimpleMethodSecurityExpressionHandler;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.ServletSemanticAuthorization;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class ServletJeapAuthorizationConfig {

    private final String systemName;
    private final ApplicationContext applicationContext;
    private final JeapMethodSecurityExpressionHandlerCustomizer expressionHandlerCustomizer;

    /**
     * Contructor to use in tests of applications that authorize against semantic roles and that need to customize the
     * method security expression handler created for this purpose by jEAP.
     *
     * @param systemName
     * @param applicationContext
     * @param expressionHandlerCustomizer
     */
    public ServletJeapAuthorizationConfig(String systemName, ApplicationContext applicationContext,
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
    public ServletJeapAuthorizationConfig(String systemName, ApplicationContext applicationContext) {
        this(systemName, applicationContext, null);
    }

    /**
     * Contructor to use in tests of applications that authorize against simple roles (i.e. not against semantic roles)
     * and that need to customize the method security expression handler created for this purpose by jEAP.
     *
     * @param applicationContext
     * @param expressionHandlerCustomizer
     */
    public ServletJeapAuthorizationConfig(ApplicationContext applicationContext,
                                          JeapMethodSecurityExpressionHandlerCustomizer expressionHandlerCustomizer) {
        this(null, applicationContext, expressionHandlerCustomizer);
    }

    /**
     * Contructor to use in tests of applications authorizing against simple roles, i.e. not against semantic roles.
     *
     * @param applicationContext
     */
    public ServletJeapAuthorizationConfig(ApplicationContext applicationContext) {
        this(applicationContext, null);
    }

    // Custom {@link MethodSecurityExpressionHandler} to add jeap role authorization security expressions
    @Bean
    public MethodSecurityExpressionHandler createExpressionHandler() {
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
    public ServletSimpleAuthorization jeapAuthorization() {
        return new ServletSimpleAuthorization();
    }

    @Bean
    public ServletSemanticAuthorization servletAuthorization() {
        return new ServletSemanticAuthorization(systemName);
    }

}
