package ch.admin.bit.jeap.security.it.resource;


import ch.admin.bit.jeap.security.resource.configuration.JeapMethodSecurityExpressionHandlerCustomizer;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;

@Configuration
public class JeapMethodSecurityHandlerCustomizerTestConfig {

    @Bean
    public JeapMethodSecurityExpressionHandlerCustomizer customizer() {
       return Mockito.spy(new PassThroughCustomizer());
    }

    private static class PassThroughCustomizer implements JeapMethodSecurityExpressionHandlerCustomizer {
        @Override
        public MethodSecurityExpressionHandler customize(DefaultMethodSecurityExpressionHandler expressionHandler) {
            return expressionHandler;
        }
    }

}
