package ch.admin.bit.jeap.log.rest;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnhandledExceptionLoggingFilterTest {

    private final UnhandledExceptionLoggingFilter filter = new UnhandledExceptionLoggingFilter();
    @SuppressWarnings("LoggerInitializedWithForeignClass")
    private final Logger filterLogger = (Logger) LoggerFactory.getLogger(UnhandledExceptionLoggingFilter.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/some/endpoint");
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void attachAppender() {
        logAppender = new ListAppender<>();
        logAppender.start();
        filterLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachAppender() {
        filterLogger.detachAppender(logAppender);
        logAppender.stop();
    }

    @Test
    void rethrowsUnhandledExceptionUnchanged() {
        RuntimeException exception = new RuntimeException("boom");
        FilterChain chain = (_, _) -> {
            throw exception;
        };

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isSameAs(exception);
    }

    @Test
    void doesNotInterfereWhenNoExceptionIsThrown() throws Exception {
        AtomicBoolean chainCalled = new AtomicBoolean();
        FilterChain chain = (_, _) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertThat(chainCalled).isTrue();
        assertThat(logAppender.list).isEmpty();
    }

    @Test
    void logsUnhandledExceptionWithRequestDetails() {
        RuntimeException exception = new RuntimeException("boom");
        FilterChain chain = (_, _) -> {
            throw exception;
        };

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isSameAs(exception);

        assertThat(logAppender.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getMessage().startsWith("An unhandled exception occurred during"));
            assertThat(event.getFormattedMessage()).contains("POST", "/some/endpoint");
            assertThat(event.getThrowableProxy().getMessage()).isEqualTo("boom");
        });
    }
}
