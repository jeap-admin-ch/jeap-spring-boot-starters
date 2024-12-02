package ch.admin.bit.jeap.log.syslog.encoder;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Slf4j
class SyslogMessagePrefixEncoderTest {

    @SuppressWarnings("RegExpRepeatedSpace")
    @Test
    void encode() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        doReturn(Level.INFO).when(event).getLevel();
        doReturn(0L).when(event).getTimeStamp();
        SyslogMessagePrefixEncoder encoder = new SyslogMessagePrefixEncoder();

        encoder.start();
        byte[] result = encoder.encode(event);

        String str = new String(result, StandardCharsets.UTF_8);
        assertTrue(str.matches("<14>Jan  1 \\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\d .*? doppler\\[\\d+]:.*"), str);
    }

    @Test
    void encode_doubleDigitDay() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        doReturn(Level.INFO).when(event).getLevel();
        doReturn(Duration.ofDays(15).toMillis()).when(event).getTimeStamp();
        SyslogMessagePrefixEncoder encoder = new SyslogMessagePrefixEncoder();

        encoder.start();
        byte[] result = encoder.encode(event);

        String str = new String(result, StandardCharsets.UTF_8);
        assertTrue(str.matches("<14>Jan 16 \\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\d .*? doppler\\[\\d+]:.*"), str);
    }
}
