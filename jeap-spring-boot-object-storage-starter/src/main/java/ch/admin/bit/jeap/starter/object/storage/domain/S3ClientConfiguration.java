package ch.admin.bit.jeap.starter.object.storage.domain;


import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.ProxyConfiguration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URISyntaxException;

import static org.springframework.util.StringUtils.hasText;


@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(S3ClientProperties.class)
@ConditionalOnExpression("${jeap.s3.client.enabled:true}")
@RequiredArgsConstructor
public class S3ClientConfiguration {

    private final S3ClientProperties properties;

    @PostConstruct
    private void postConstruct() {
        log.debug("Initialized s3Client with connection properties {}.", properties.toString());
    }

    @Bean
    @ConditionalOnMissingBean
    public S3Client s3Client(S3ClientProperties properties, AwsCredentialsProvider awsCredentialsProvider) throws URISyntaxException {
            S3ClientBuilder s3ClientBuilder = S3Client.builder()
                    .region(properties.getRegion())
                    .serviceConfiguration(serviceConfiguration())
                    .httpClient(UrlConnectionHttpClient.builder()
                            .proxyConfiguration(ProxyConfiguration.builder() // Configure proxy to work around the issue https://github.com/aws/aws-sdk-java-v2/issues/4728 which is coming with the aws sdk update
                                    .useSystemPropertyValues(false)
                                    .useEnvironmentVariablesValues(false)
                                    .build())
                            .build())
                    .credentialsProvider(awsCredentialsProvider);

            if (hasText(properties.getEndpointUrl())) {
                s3ClientBuilder = s3ClientBuilder.endpointOverride(properties.buildProtocolAwareEndpointUrl());
            }
            if (properties.getAccessKey() != null && properties.getSecretKey() != null) {
                log.debug("Creating AwsCredentialsProvider using configured accessKey and secretKey...");
                s3ClientBuilder = s3ClientBuilder.credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey()))
                );
            }
            return s3ClientBuilder.build();
    }

    private S3Configuration serviceConfiguration() {
        return S3Configuration.builder().pathStyleAccessEnabled(true).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public AwsCredentialsProvider credentialsProvider() {
        return DefaultCredentialsProvider.create();
    }
}
