package ch.admin.bit.jeap.security.resource.introspection;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Captures the messages logged by a logger (and the loggers below it) on all levels for the duration of a test,
 * restoring the level of the logger when closed. Safe for messages logged by other threads, e.g. by Caffeine's
 * maintenance.
 */
final class LogCapture implements AutoCloseable {

    private final Logger logger;
    private final Level previousLevel;
    private final List<ILoggingEvent> events = new CopyOnWriteArrayList<>();
    private final AppenderBase<ILoggingEvent> appender = new AppenderBase<>() {
        @Override
        protected void append(ILoggingEvent event) {
            events.add(event);
        }
    };

    @SuppressWarnings("SameParameterValue")
    static LogCapture of(Class<?> loggedClass) {
        return of(loggedClass.getName());
    }

    static LogCapture of(String loggerName) {
        return new LogCapture((Logger) LoggerFactory.getLogger(loggerName));
    }

    private LogCapture(Logger logger) {
        this.logger = logger;
        this.previousLevel = logger.getLevel();
        logger.setLevel(Level.TRACE);
        appender.start();
        logger.addAppender(appender);
    }

    /**
     * The formatted messages logged on the given level, in the order they have been logged.
     */
    @SuppressWarnings("SameParameterValue")
    List<String> messages(Level level) {
        return events.stream()
                .filter(event -> event.getLevel().equals(level))
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    @Override
    public void close() {
        logger.detachAppender(appender);
        appender.stop();
        logger.setLevel(previousLevel);
    }

}
