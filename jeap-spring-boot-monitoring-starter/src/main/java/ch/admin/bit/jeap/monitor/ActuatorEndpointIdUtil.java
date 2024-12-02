package ch.admin.bit.jeap.monitor;

import lombok.experimental.UtilityClass;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;

import java.util.Optional;

@UtilityClass
class ActuatorEndpointIdUtil {

    static Optional<String> getEndpointId(Class<?> endPointClass) {
        MergedAnnotation<Endpoint> annotation = MergedAnnotations.from(endPointClass).get(Endpoint.class);
        if (annotation.isPresent()) {
            return Optional.of(annotation.getString("id"));
        }
        else {
            return Optional.empty() ;
        }
    }

    static boolean isLoggersEndpoint(Class<?> endpointClass) {
        return getEndpointId(endpointClass).map(id -> id.equals("loggers") ).orElse(false);
    }

}
