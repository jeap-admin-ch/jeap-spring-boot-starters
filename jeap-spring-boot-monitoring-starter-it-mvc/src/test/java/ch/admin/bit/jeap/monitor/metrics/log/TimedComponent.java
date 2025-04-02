package ch.admin.bit.jeap.monitor.metrics.log;

import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TimedComponent {

    @Timed("timed_method")
    public void timedMethod() {
        log.info("Executing timed method");
    }
}
