package ch.admin.bit.jeap.log.syslog;

import ch.admin.bit.jeap.log.syslog.connection.TLSSyslogConnection;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.EncoderBase;
import ch.qos.logback.core.spi.ContextAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TLSSyslogAppenderTest {

    private static final String LOGGED_MESSAGE = "logged message";

    @Mock
    private TLSSyslogConnection tlsSyslogConnection;
    @Mock
    private ILoggingEvent loggingEvent;
    private final List<String> transmittedMessages = new ArrayList<>();

    private TLSSyslogAppender tlsSyslogAppender;

    @Test
    void append_when_syslogTransmitIsSuccesful_then_shouldTransmitMessage() {
        stubSuccessfulSyslogConnectionTransmit();

        tlsSyslogAppender.append(loggingEvent);

        verify(tlsSyslogConnection).attemptConnection();
        assertEquals(1, transmittedMessages.size());
        assertEquals(LOGGED_MESSAGE, transmittedMessages.get(0));
    }

    @Test
    void append_when_messageExceedsMaxSize_then_shouldTruncateMessage() {
        tlsSyslogAppender.setMaxMessageSize(3);
        stubSuccessfulSyslogConnectionTransmit();

        tlsSyslogAppender.append(loggingEvent);

        assertEquals(1, transmittedMessages.size());
        assertEquals(LOGGED_MESSAGE.substring(0, 3), transmittedMessages.get(0));
    }

    private void stubSuccessfulSyslogConnectionTransmit() {
        doAnswer(invocation -> transmittedMessages.add(new String(invocation.getArgument(0), StandardCharsets.UTF_8)))
                .when(tlsSyslogConnection).transmit(any(byte[].class));
    }

    @BeforeEach
    void beforeEach() {
        Encoder<ILoggingEvent> encoder = new EncoderStub();
        when(tlsSyslogConnection.attemptConnection()).thenReturn(true);

        tlsSyslogAppender = new TLSSyslogAppender() {
            @Override
            protected TLSSyslogConnection createSyslogConnection(ContextAware contextAware) {
                return tlsSyslogConnection;
            }
        };
        tlsSyslogAppender.setSyslogHost("host");
        tlsSyslogAppender.setPort(1234);
        tlsSyslogAppender.setEncoder(encoder);
        tlsSyslogAppender.start();
        when(loggingEvent.getFormattedMessage()).thenReturn(LOGGED_MESSAGE);
        TLSSyslogAppender.cleanConnectionCache();
    }

    private static class EncoderStub extends EncoderBase<ILoggingEvent> {
        @Override
        public byte[] encode(ILoggingEvent event) {
            return event.getFormattedMessage().getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public byte[] headerBytes() {
            return null;
        }

        @Override
        public byte[] footerBytes() {
            return null;
        }
    }
}
