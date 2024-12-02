package ch.admin.bit.jeap.starter.object.storage.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import software.amazon.awssdk.regions.Region;

import java.net.URI;
import java.net.URISyntaxException;

@Getter
@Setter
@ToString
@ConfigurationProperties(prefix = "jeap.s3.client")
public class S3ClientProperties {

    private static final Logger log = LoggerFactory.getLogger(S3ClientProperties.class);

    /**
     * Enables / Disables the S3Client
     */
    private Boolean enabled = true;

    /**
     * The base URL of the S3 provider.
     * Consisting of host and port. Optionally the protocol, but the SSL flag is then overwritten.
     */
    private String endpointUrl;

    /**
     * The Amazon Web Services region that hosts the service.
     */
    private Region region = Region.AWS_GLOBAL;

    /**
     * The access key for connecting to S3 with AWS SDK.
     * <p>
     * (excluded from toString for security reasons)
     */
    @ToString.Exclude
    private String accessKey;

    /**
     * The secret access key for connecting to S3 with AWS SDK.
     * <p>
     * (excluded from toString for security reasons)
     */
    @ToString.Exclude
    private String secretKey;

    /**
     * If set to false it will connect to endpointUrl through http instead of https. This is
     * needed only for local development where S3 is provided through docker (localstack).
     */
    private boolean tls = true;

    /**
     * Returns the final endpoint url which can be passed to AWS Client Config. It considers ssl and
     * port configuration above. Examples are
     * <ul>
     *   <li>http://localhost:4566 (local development)</li>
     *   <li>https://host.admin.ch</li>
     * </ul>
     */
    public URI buildProtocolAwareEndpointUrl() throws URISyntaxException {
        if (endpointUrl.startsWith("http://") || endpointUrl.startsWith("https://")) {
            return URI.create(endpointUrl);
        }
        var protocol = isTls() ? "https://" : "http://";
        return new URI(protocol + endpointUrl);
    }
}
