package ch.admin.bit.jeap.config.aws.secretsmanager.config;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.env.EnumerablePropertySource;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Notice: This class is based on code from the Spring Cloud AWS project, which is licensed under the Apache License 2.0
 * and available at <a href="https://github.com/awspring/spring-cloud-aws">github.com/awspring/spring-cloud-aws</a>.
 */
public class AwsSecretsManagerPropertySource extends EnumerablePropertySource<SecretsManagerClient> {

    @SuppressWarnings("FieldMayBeFinal")
    private static Log LOG = LogFactory.getLog(AwsSecretsManagerPropertySource.class);
    private static final String PREFIX_PART = "?prefix=";

    private final ObjectMapper jsonMapper = new ObjectMapper();

    /**
     * Optional prefix that gets added to resolved property keys. Useful to identify properties that are coming from
     * AWS Secrets Manager.
     */
    private final String prefix;
    private final String secretId;

    private final Map<String, Object> properties = new LinkedHashMap<>();

    public AwsSecretsManagerPropertySource(AwsSecretsManagerConfigDataResource resource, SecretsManagerClient client) {
        super(AwsSecretsManagerConfigDataLoader.AWS_SECRETSMANAGER + resource.getSecretName(), client);
        this.secretId = resolveSecretId(resource.getSecretName());
        this.prefix = resolvePrefix(resource.getSecretName());
    }

    private static String resolvePrefix(String context) {
        int prefixIndex = context.indexOf(PREFIX_PART);
        if (prefixIndex != -1) {
            return context.substring(prefixIndex + PREFIX_PART.length());
        }
        return null;
    }

    private static String resolveSecretId(String context) {
        int prefixIndex = context.indexOf(PREFIX_PART);
        if (prefixIndex != -1) {
            return context.substring(0, prefixIndex);
        }
        return context;
    }

    @Override
    public String[] getPropertyNames() {
        Set<String> strings = properties.keySet();
        return strings.toArray(new String[0]);
    }

    @Override
    public Object getProperty(String name) {
        return properties.get(name);
    }

    public void initPropertiesFromAwsSecretsManager() {
        GetSecretValueRequest secretValueRequest = GetSecretValueRequest.builder().secretId(secretId).build();
        GetSecretValueResponse secretValueResponse = source.getSecretValue(secretValueRequest);
        if (secretValueResponse.secretString() != null) {
            try {
                Map<String, Object> secretMap = jsonMapper.readValue(secretValueResponse.secretString(),
                        new TypeReference<>() {
                        });
                for (Map.Entry<String, Object> secretEntry : secretMap.entrySet()) {
                    LOG.debug("Populating property retrieved from AWS Secrets Manager: " + secretEntry.getKey());
                    String propertyKey = prefix != null ? prefix + secretEntry.getKey() : secretEntry.getKey();
                    properties.put(propertyKey, secretEntry.getValue());
                }
            } catch (JsonParseException e) {
                // If the secret is not a JSON string, then it is a simple "plain text" string
                String[] parts = secretValueResponse.name().split("/");
                String secretName = parts[parts.length - 1];
                LOG.debug("Populating property retrieved from AWS Secrets Manager: " + secretName);
                String propertyKey = prefix != null ? prefix + secretName : secretName;
                properties.put(propertyKey, secretValueResponse.secretString());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            String[] parts = secretValueResponse.name().split("/");
            String secretName = parts[parts.length - 1];
            LOG.debug("Populating property retrieved from AWS Secrets Manager: " + secretName);
            String propertyKey = prefix != null ? prefix + secretName : secretName;
            properties.put(propertyKey, secretValueResponse.secretBinary().asByteArray());
        }
    }
}
