package ch.admin.bit.jeap.config.aws.appconfig;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static ch.admin.bit.jeap.config.aws.appconfig.JeapAWSAppConfigProperties.JEAP_AWS_CONFIG_PREFIX;

@Data
@ConfigurationProperties(JEAP_AWS_CONFIG_PREFIX)
public class JeapAWSAppConfigProperties {

    public static final String JEAP_AWS_CONFIG_PREFIX = "jeap.config.aws.appconfig";

    /**
     * Name of the environment
     */
    private String envId;

    /**
     * (Optional) Establishes a minimum interval between polls for the latest configuration and at the same time is used
     * by AWS as the standard poll interval. Valid Range: Minimum value of 15. Maximum value of 86400. Defaults to 60.
     */
    private Integer requiredMinimumPollIntervalInSeconds;

    /**
     * (Optional) Do not load data from a 'common' application in the default location configuration.
     */
    private boolean noDefaultCommonConfig = false;

    /**
     * (Optional) Do not load data from a 'common-platform' application in the default location configuration.
     */
    private boolean noDefaultCommonPlatformConfig = false;

    /**
     * Trust all certificates when connecting to AWS AppConfig. Set to <code>true</code> for development purposes only.
     */
    private boolean trustAllCertificates = false;

}
