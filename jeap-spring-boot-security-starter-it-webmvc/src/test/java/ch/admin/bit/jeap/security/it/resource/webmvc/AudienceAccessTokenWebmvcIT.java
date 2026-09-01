package ch.admin.bit.jeap.security.it.resource.webmvc;

import ch.admin.bit.jeap.security.it.resource.AbstractAudienceAccessTokenIT;
import ch.admin.bit.jeap.security.it.resource.AbstractStrictAudienceValidationOffIT;
import ch.admin.bit.jeap.security.it.resource.AbstractStrictAudienceValidationOnIT;
import ch.admin.bit.jeap.security.it.resource.AbstractStrictAudienceValidationWarnIT;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Runs the audience validation tests once for each strict audience validation mode. The test methods live in the
 * mode-specific abstract classes and their common base class {@link AbstractAudienceAccessTokenIT}, the nested
 * classes only provide the accordingly configured application contexts.
 */
class AudienceAccessTokenWebmvcIT {

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
            "server.port=8003"
            // strict audience validation not configured -> defaults to off
    })
    class StrictAudienceValidationOff extends AbstractStrictAudienceValidationOffIT {
        StrictAudienceValidationOff(@Value("${server.port}") int serverPort, @Value("${spring.application.name}") String context) {
            super(serverPort, context);
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
            "server.port=8033",
            "jeap.security.oauth2.resourceserver.strict-audience-validation=on"})
    class StrictAudienceValidationOn extends AbstractStrictAudienceValidationOnIT {
        StrictAudienceValidationOn(@Value("${server.port}") int serverPort, @Value("${spring.application.name}") String context) {
            super(serverPort, context);
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
            "server.port=8034",
            "jeap.security.oauth2.resourceserver.strict-audience-validation=warn"})
    class StrictAudienceValidationWarn extends AbstractStrictAudienceValidationWarnIT {
        StrictAudienceValidationWarn(@Value("${server.port}") int serverPort, @Value("${spring.application.name}") String context) {
            super(serverPort, context);
        }
    }

}
