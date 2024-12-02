package ch.admin.bit.jeap.log.syslog.encoder;

import ch.qos.logback.classic.pattern.SyslogStartConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.LevelToSyslogSeverity;
import ch.qos.logback.core.net.SyslogConstants;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Modelled after {@link SyslogStartConverter}, but providing timestamps accurate to the millisecond. While not entirely
 * BSD syslog compliant, this format is supported by the log relay and ensures correct ordering of log entries in Splunk.
 */
class SyslogMillisecondsPrefix {

    /**
     * Currently, the following prefix is necessary to activate a Splunk input parser that correctly parses
     * escapes inside JSON values (i.e. " in a json string). Otherwise, when e.g. a logged HTTP header in a json
     * value contains a ", Splunk will not receive a valid JSON document and be unable to index the contained attributes.
     * <a href="https://confluence.bit.admin.ch/display/CCPS/2021/10/19/%5BBugfix%5D+CloudFoundry+Logging+-+App+-%3E+Logrelay%3A+incorrectly+formatted+JSON">See here for details</a>.
     */
    private static final String JSON_PARSING_WORKAROUND_PREFIX = "11111111-";

    private final DateTimeFormatter dateTimeFormatterDoubleDigitDay = DateTimeFormatter.ofPattern("MMM dd HH:mm:ss.SSS", Locale.US);
    /**
     * <a href="https://datatracker.ietf.org/doc/html/rfc3164#page-10">rfc3164</a>:
     * "If the day of the month is less than 10, then it MUST be represented as a space and then the number."
     */
    private final DateTimeFormatter dateTimeFormatterSingleDigitDay = DateTimeFormatter.ofPattern("MMM  d HH:mm:ss.SSS", Locale.US);

    private String localHostName;

    void start() {
        localHostName = getHostPrefix();
    }

    String getPrefix(ILoggingEvent event) {
        int pri = SyslogConstants.LOG_USER + LevelToSyslogSeverity.convert(event);

        return "<" + pri + ">" +
               computeTimeStampString(event.getTimeStamp()) +
               ' ' + localHostName + ' ';
    }

    private String getHostPrefix() {
        String hostName = getLocalHostname();
        if (hostName.length() > JSON_PARSING_WORKAROUND_PREFIX.length()) {
            hostName = JSON_PARSING_WORKAROUND_PREFIX + hostName.substring(JSON_PARSING_WORKAROUND_PREFIX.length());
        } else {
            hostName = JSON_PARSING_WORKAROUND_PREFIX + hostName;
        }
        return hostName;
    }

    /**
     * This method gets the network name of the machine we are running on.
     * Returns "UNKNOWN_LOCALHOST" in the unlikely case where the host name
     * cannot be found.
     *
     * @return String the name of the local host
     */
    String getLocalHostname() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            return addr.getHostName();
        } catch (UnknownHostException uhe) {
            return "UNKNOWN_LOCALHOST";
        }
    }

    private String computeTimeStampString(long millis) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
        return localDateTime.getDayOfMonth() < 10 ?
                dateTimeFormatterSingleDigitDay.format(localDateTime) :
                dateTimeFormatterDoubleDigitDay.format(localDateTime);
    }
}
