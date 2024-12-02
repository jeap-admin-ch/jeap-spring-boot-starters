package ch.admin.bit.jeap.log.syslog.encoder;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class SyslogMillisecondsPrefixTest {

    @Test
    void getPrefix() {
        SyslogMillisecondsPrefix syslogMillisecondsPrefix = new SyslogMillisecondsPrefix() {
            @Override
            String getLocalHostname() {
                return "f99f516a-32bb-49a0-831a-130f4b3a30cd";
            }
        };
        syslogMillisecondsPrefix.start();
        ILoggingEvent event = mock(ILoggingEvent.class);
        doReturn(Level.INFO).when(event).getLevel();
        doReturn(0L).when(event).getTimeStamp();

        String prefix = syslogMillisecondsPrefix.getPrefix(event);

        assertTrue(prefix.matches("<14>Jan {2}1 \\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\d 11111111-32bb-49a0-831a-130f4b3a30cd "), prefix);
    }

    @Test
    void getPrefix_shortHostname() {
        SyslogMillisecondsPrefix syslogMillisecondsPrefix = new SyslogMillisecondsPrefix() {
            @Override
            String getLocalHostname() {
                return "myhost";
            }
        };
        syslogMillisecondsPrefix.start();
        ILoggingEvent event = mock(ILoggingEvent.class);
        doReturn(Level.INFO).when(event).getLevel();
        doReturn(0L).when(event).getTimeStamp();

        String prefix = syslogMillisecondsPrefix.getPrefix(event);

        assertTrue(prefix.matches("<14>Jan {2}1 \\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\d 11111111-myhost "), prefix);
    }
}