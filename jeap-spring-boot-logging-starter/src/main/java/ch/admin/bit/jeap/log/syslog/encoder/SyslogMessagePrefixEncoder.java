package ch.admin.bit.jeap.log.syslog.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.EncoderBase;

import java.nio.charset.StandardCharsets;

/**
 * A logback {@link ch.qos.logback.core.encoder.Encoder} providing a message prefix according to the syslog BSD format
 * as specified in <a href="https://datatracker.ietf.org/doc/html/rfc3164#page-8">RFC3164</a>.
 */
public class SyslogMessagePrefixEncoder extends EncoderBase<ILoggingEvent> {
    /**
     * <code>doppler[123]: </code> is mandatory for the logrelay to forward logs to Splunk
     */
    private final String tag = "doppler[" + ProcessHandle.current().pid() + "]:";
    private final SyslogMillisecondsPrefix sysloogPrefix;

    public SyslogMessagePrefixEncoder() {
        sysloogPrefix = new SyslogMillisecondsPrefix();
    }

    @Override
    public void start() {
        sysloogPrefix.start();
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public byte[] headerBytes() {
        return null;
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        String prefix = sysloogPrefix.getPrefix(event);
        String prefixWithTag = prefix + tag + " ";
        return prefixWithTag.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] footerBytes() {
        return null;
    }
}
