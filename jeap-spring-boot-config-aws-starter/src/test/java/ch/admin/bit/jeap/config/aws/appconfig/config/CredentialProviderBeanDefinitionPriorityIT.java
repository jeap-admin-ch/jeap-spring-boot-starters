package ch.admin.bit.jeap.config.aws.appconfig.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class CredentialProviderBeanDefinitionPriorityIT {

    @Autowired
    private AwsCredentialsProvider awsCredentialsProvider;

    @Test
    void contextLoads() {
        assertThat(awsCredentialsProvider)
                .isInstanceOf(DefaultCredentialsProvider.class);
    }
}
