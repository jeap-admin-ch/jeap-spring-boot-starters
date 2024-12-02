package ch.admin.bit.jeap.log.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

/**
 * This Logback-TurboFilter is used to Filter suppress a LogMessage which contains Text.
 * Which LogMessage has to be suppressed is declarated in logback-spring.xml as follow:
 * <pre>
 *  {@code
 *  <turboFilter class="ch.admin.bit.jeap.log.filter.LogMessageFilter">
 *     <LogMessage>Found no committed offset for partition</LogMessage>
 *  </turboFilter>
 *  }
 *  </pre>
 */
public class LogMessageFilter extends TurboFilter {

    private String logMessage;

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        if (logMessage == null)  {
            return FilterReply.NEUTRAL;
        } else if ((format != null) && (format.contains(logMessage))) {
            return FilterReply.DENY;
        } else {
            return FilterReply.NEUTRAL;
        }
    }

    public void setLogMessage(String logMessage) {
        this.logMessage = logMessage;
    }


}
