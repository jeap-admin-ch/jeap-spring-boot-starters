package ch.admin.bit.jeap.security.it.bearertoken;

import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.util.AnnotationUtils;

import java.lang.reflect.Parameter;

public class BearerTokenTestExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(BearerTokenTestExtension.class);
    private static final String BEARER_TOKEN_RESOURCE_KEY = "bearerTokenResource";

    @Override
    public void beforeAll(ExtensionContext context) {
        BearerTokenTestResource bearerTokenResource = new BearerTokenTestResource();
        bearerTokenResource.start();
        storeBearerTokenResource(context, bearerTokenResource);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        BearerTokenTestResource bearerTokenResource = retrieveBearerTokenResource(context);
        if (bearerTokenResource != null) {
            bearerTokenResource.stop();
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        BearerTokenTestResource bearerTokenResource = retrieveBearerTokenResource(context);
        if (bearerTokenResource != null) {
            bearerTokenResource.reset();
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        BearerTokenTestResource bearerTokenResource = retrieveBearerTokenResource(context);
        if (bearerTokenResource != null) {
            bearerTokenResource.reset();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context) throws ParameterResolutionException {
        Parameter parameter = parameterContext.getParameter();
        return parameter.getType() == String.class &&
                AnnotationUtils.isAnnotated(parameter, BearerTokenUrl.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) throws ParameterResolutionException {
        BearerTokenTestResource bearerTokenResource = retrieveBearerTokenResource(context);
        if (bearerTokenResource == null) {
            throw new ParameterResolutionException("BearerTokenTestResource not available");
        }
        return bearerTokenResource.getBearerTokenUrl();
    }

    private static void storeBearerTokenResource(ExtensionContext context, BearerTokenTestResource bearerTokenResource) {
        context.getStore(NAMESPACE).put(BEARER_TOKEN_RESOURCE_KEY, bearerTokenResource);
    }

    private static BearerTokenTestResource retrieveBearerTokenResource(ExtensionContext context) {
        return context.getStore(NAMESPACE).get(BEARER_TOKEN_RESOURCE_KEY, BearerTokenTestResource.class);
    }
}
