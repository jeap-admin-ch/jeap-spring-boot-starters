package ch.admin.bit.jeap.config.aws.secretsmanager;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import java.net.URI;

@Data
@ConfigurationProperties(JeapAwsSecretsManagerProperties.CONFIG_PREFIX)
public class JeapAwsSecretsManagerProperties {

    public static final String CONFIG_PREFIX = "jeap.aws.secretsmanager";

    private String endpointOverride = null;
    private String region = null;
    private boolean enabled = true;
    private String accessKeyId = null;
    private String secretAccessKey = null;

    public URI getEndpointOverrideUri() {
        return this.endpointOverride == null ? null : URI.create(this.endpointOverride);
    }

    public Region getRegionOverride() {
        return region == null ? null : Region.of(region);
    }

    /**
     * Usually, the {@link DefaultCredentialsProvider} will suffice as it will use the default AWS credentials provider chain.
     * For tests or special cases, you can provide your own credentials provider.
     */
    public AwsCredentialsProvider getCredentialsProvider() {
        if (accessKeyId != null && secretAccessKey != null) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey));
        }
        return DefaultCredentialsProvider.create();
    }
}
