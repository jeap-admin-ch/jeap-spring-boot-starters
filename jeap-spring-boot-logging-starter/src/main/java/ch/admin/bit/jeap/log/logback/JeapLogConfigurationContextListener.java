package ch.admin.bit.jeap.log.logback;

public class JeapLogConfigurationContextListener extends AbstractContextListenerBase {

    static final String JEAP_LOGGING_PLATFORM = "jeapLoggingPlatform";
    static final String CLOUDWATCH_PLATFORM_VALUE = "cloudwatch";
    static final String RHOS_PLATFORM_VALUE = "rhos";
    static final String LOGRELAY_PROFILE = "logrelayProfile";
    static final String CLOUD_PROFILE = "cloudProfile";
    static final String ROLLING_LOG_FILE_PROFILE = "rollingLogFileProfile";
    static final String ADMIN_ENABLED = "adminEnabled";
    static final String ADMIN_URL = "adminUrl";
    static final String TRUE = "true";

    static final String CLOUDWATCH_APPENDER = "cloudwatch";
    static final String RHOS_APPENDER = "rhos";
    static final String LOGRELAY_APPENDER = "logrelay";
    static final String CONSOLEJSON_APPENDER = "consolejson";
    static final String CONSOLETEXT_APPENDER = "consoletext";
    static final String ROLLINGFILE_APPENDER = "rollingfile";

    @Override
    public void start() {
        if (isCloudwatch()) {
            context.putProperty(CLOUDWATCH_APPENDER, TRUE);
        } else if (isRhos()) {
            context.putProperty(RHOS_APPENDER, TRUE);
        }
        else {
            if (isLogrelay()) {
                context.putProperty(LOGRELAY_APPENDER, TRUE);
            } else if (isCloud()) {
                context.putProperty(CONSOLEJSON_APPENDER, TRUE);
            } else {
                context.putProperty(CONSOLETEXT_APPENDER, TRUE);
            }
        }

        if (isTrue(ADMIN_ENABLED) || (hasAdminUrl() && !adminDisabled()) || isTrue(ROLLING_LOG_FILE_PROFILE)) {
            context.putProperty(ROLLINGFILE_APPENDER, TRUE);
        }
    }

    private boolean isCloudwatch() {
        return isWantedPlatform(CLOUDWATCH_PLATFORM_VALUE);
    }

    private boolean isRhos() {
        return isWantedPlatform(RHOS_PLATFORM_VALUE);
    }

    private boolean isCloud() {
        return isTrue(CLOUD_PROFILE);
    }

    private boolean adminDisabled() {
        String value = context.getProperty(ADMIN_ENABLED);
        return "false".equalsIgnoreCase(value);
    }

    private boolean isLogrelay() {
        return isTrue(LOGRELAY_PROFILE);
    }

    private boolean hasAdminUrl() {
        String value = context.getProperty(ADMIN_URL);
        return value != null && !value.isBlank();
    }

    private boolean isTrue(String propertyName) {
        return Boolean.parseBoolean(context.getProperty(propertyName));
    }

    private boolean isWantedPlatform(String wantedPlatform) {
        String actualValue = context.getProperty(JEAP_LOGGING_PLATFORM);
        return wantedPlatform.equalsIgnoreCase(actualValue);
    }
}
