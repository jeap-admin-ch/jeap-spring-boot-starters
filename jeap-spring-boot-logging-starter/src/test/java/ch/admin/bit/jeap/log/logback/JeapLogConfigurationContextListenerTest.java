package ch.admin.bit.jeap.log.logback;

import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashSet;
import java.util.Set;

import static ch.admin.bit.jeap.log.logback.JeapLogConfigurationContextListener.*;
import static org.assertj.core.api.Assertions.assertThat;

class JeapLogConfigurationContextListenerTest {

    private JeapLogConfigurationContextListener listener;
    private LoggerContext context;

    @BeforeEach
    void setUp() {
        listener = new JeapLogConfigurationContextListener();
        context = new LoggerContext();
        listener.setContext(context);
    }

    @Test
    void noPropertiesOrProfiles_shouldLogToConsoleAsText() {
        listener.start();

        assertOnlyActiveAppender(CONSOLETEXT_APPENDER);
    }

    private void assertOnlyActiveAppender(String activeAppender) {
        Set<String> disabledAppenders = new HashSet<>(Set.of(
                CLOUDWATCH_APPENDER,
                CONSOLEJSON_APPENDER,
                CONSOLETEXT_APPENDER,
                LOGRELAY_APPENDER,
                ROLLINGFILE_APPENDER));
        disabledAppenders.remove(activeAppender);

        assertThat(context.getProperty(activeAppender))
                .isEqualTo(TRUE);
        disabledAppenders.forEach(disabledAppender ->
                assertThat(context.getProperty(disabledAppender))
                        .isNull());
    }

    @Test
    void cloudProfileActive_shouldLogToConsoleAsJson() {
        context.putProperty(CLOUD_PROFILE, TRUE);

        listener.start();

        assertOnlyActiveAppender(CONSOLEJSON_APPENDER);
    }

    @Test
    void cloudwatchPlatform_shouldLogToConsoleAsJsonForCloudwatch() {
        context.putProperty(JEAP_LOGGING_PLATFORM, CLOUDWATCH_PLATFORM_VALUE);

        listener.start();

        assertOnlyActiveAppender(CLOUDWATCH_APPENDER);
    }

    @ParameterizedTest
    @CsvSource({
            "cloudProfile, true",
            "logrelayProfile, true"})
    void cloudwatchPlatform_shouldLogToConsoleAsJsonForCloudwatch_regardlessOfOtherProfiles(String key, String value) {
        context.putProperty(JEAP_LOGGING_PLATFORM, CLOUDWATCH_PLATFORM_VALUE);
        context.putProperty(key, value);

        listener.start();

        assertOnlyActiveAppender(CLOUDWATCH_APPENDER);
    }

    @Test
    void logRelayProfileActive_shouldLogToLogrelay() {
        context.putProperty(LOGRELAY_PROFILE, TRUE);

        listener.start();

        assertOnlyActiveAppender(LOGRELAY_APPENDER);
    }

    @ParameterizedTest
    @CsvSource({
            "cloudProfile, true",
            "logrelayProfile, true"})
    void rollingLogFileProfileActive_shouldAlsoLogToLogfile(String key, String value) {
        context.putProperty(ROLLING_LOG_FILE_PROFILE, TRUE);
        context.putProperty(key, value);

        listener.start();

        assertThat(context.getProperty(ROLLINGFILE_APPENDER))
                .isEqualTo(TRUE);
    }

    @ParameterizedTest
    @CsvSource({
            "rollingLogFileProfile, true",
            "adminUrl, somevalue",
            "adminEnabled, true"})
    void rollingFileEnabled_shouldAlsoLogToLogfile(String key, String value) {
        context.putProperty(key, value);

        listener.start();

        assertThat(context.getProperty(ROLLINGFILE_APPENDER))
                .isEqualTo(TRUE);
    }

    @Test
    void rollingFile_shouldNotLogToFileIfAdminDisabled() {
        context.putProperty(ADMIN_URL, "somevalue");
        context.putProperty(ADMIN_ENABLED, "false");

        listener.start();

        assertThat(context.getProperty(ROLLING_LOG_FILE_PROFILE))
                .isNull();
    }

    @Test
    void rollingFile_shouldLogToFileIfAdminEnabled() {
        context.putProperty(ADMIN_URL, "somevalue");
        context.putProperty(ADMIN_ENABLED, TRUE);

        listener.start();

        assertThat(context.getProperty(ROLLINGFILE_APPENDER))
                .isEqualTo(TRUE);
    }
}