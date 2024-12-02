package ch.admin.bit.jeap.postgresql.aws;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

@SpringBootApplication
public class TestConfig {

    @Bean
    public AwsCredentialsProvider testCredentialsProvider() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create("myAccessKeyId", "mySecretAccessKey");
        return StaticCredentialsProvider.create(credentials);
    }

    @Bean
    public MeterRegistry registry() {
        return new SimpleMeterRegistry();
    }
}
