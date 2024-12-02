package ch.admin.bit.jeap.postgresql.aws;

import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Getter
@Setter
public class RDSDataSource extends HikariDataSource {

    public static final int DEFAULT_PORT_NUMBER = 5432;
    public static final String DEFAULT_HOSTNAME = "localhost";
    public static final String ERROR_MESSAGE_AFTER_CONFIGURATION_SEALED = "The configuration of the pool is sealed once started. Use HikariConfigMXBean for runtime changes.";

    private String region;
    private String hostname;
    private String port;
    private AwsCredentialsProvider credentialsProvider;

    @Override
    public String getPassword() {
        if (hasText(super.getPassword())){
            return super.getPassword();
        }
        RdsUtilities utilities = RdsUtilities.builder()
                .region(Region.of(region))
                .build();

        GenerateAuthenticationTokenRequest authenticationTokenRequest = GenerateAuthenticationTokenRequest.builder()
                .credentialsProvider(this.credentialsProvider)
                .username(this.getUsername())
                .port(this.getPort())
                .hostname(this.getHostname())
                .build();

        if (log.isDebugEnabled()) {
            log.debug("Obtaining IAM Token for RDS...");
        }
        return utilities.generateAuthenticationToken(authenticationTokenRequest);
    }

    public String getHostname() {
        if (hasText(this.hostname)) {
            return this.hostname;
        } else {
            if (hasText(getJdbcUrl())) {
                if (getJdbcUrl().contains("//")) {
                    var slashing = getJdbcUrl().indexOf("//") + 2;
                    int indexOfSlash = getJdbcUrl().indexOf("/", slashing);
                    if (indexOfSlash != -1) {
                        return getJdbcUrl().substring(slashing, indexOfSlash).split(":")[0];
                    } else {
                        return getJdbcUrl().substring(slashing).split(":")[0];
                    }
                }
            }
            return DEFAULT_HOSTNAME;
        }
    }

    public int getPort() {
        if (hasText(this.port)) {
            return Integer.parseInt(port);
        } else {
            if (hasText(getJdbcUrl())) {
                if (getJdbcUrl().contains("//")) {
                    var slashing = getJdbcUrl().indexOf("//") + 2;
                    int indexOfSlash = getJdbcUrl().indexOf("/", slashing);
                    String substring;
                    if (indexOfSlash != -1) {
                        substring = getJdbcUrl().substring(slashing, indexOfSlash);
                    } else {
                        substring = getJdbcUrl().substring(slashing);
                    }
                    String[] hostnameParts = substring.split(":");
                    if (hostnameParts.length == 2) {
                        return Integer.parseInt(hostnameParts[1]);
                    }
                }
            }
            return DEFAULT_PORT_NUMBER;
        }
    }

    @Override
    public void setSchema(String schema) {
        ignoreErrorsAfterSealed(() -> super.setSchema(schema), "schema");
    }

    @Override
    public void setPoolName(String poolName) {
        ignoreErrorsAfterSealed(() -> super.setPoolName(poolName), "poolName");
    }

    private void ignoreErrorsAfterSealed(Runnable runnable, String parameterName) {
        try {
            runnable.run();
        } catch (IllegalStateException ise) {
            if (!ERROR_MESSAGE_AFTER_CONFIGURATION_SEALED.equals(ise.getMessage())) {
                throw ise;
            } else {
                log.trace("Attempted to modify parameter '{}' after HikariConfiguration has been sealed. Ignoring.", parameterName);
            }
        }
    }
}
