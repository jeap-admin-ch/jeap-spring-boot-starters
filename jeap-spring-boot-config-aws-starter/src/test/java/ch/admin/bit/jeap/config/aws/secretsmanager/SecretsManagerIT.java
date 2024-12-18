package ch.admin.bit.jeap.config.aws.secretsmanager;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.ExecConfig;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SECRETSMANAGER;

@Slf4j
@Testcontainers
public class SecretsManagerIT {

    @Container
    static public LocalStackContainer localStackContainer = createLocalStackContainer();

    @SuppressWarnings("resource")
    private static LocalStackContainer createLocalStackContainer() {
        return new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.0.3")
                .asCompatibleSubstituteFor("localstack/localstack"))
                .withExposedPorts(4566)
                .withServices(SECRETSMANAGER)
                .withEnv("DISABLE_EVENTS", "1") // Disable localstack features that require an internet connection
                .withEnv("SKIP_INFRA_DOWNLOADS", "1")
                .withEnv("SKIP_SSL_CERT_DOWNLOAD", "1");
    }

    @BeforeAll
    static void beforeAll() throws IOException, InterruptedException {
        createSecret("secret1");
        createSecret("secret2");
    }

    private static void createSecret(String secretName) throws IOException, InterruptedException {

        String[] cmds = {"awslocal", "secretsmanager", "create-secret", "--name", secretName,
                "--secret-string", secretName + "Value", "--region", localStackContainer.getRegion()};
        ExecResult execResult = localStackContainer.execInContainer(ExecConfig.builder()
                .command(cmds)
                .envVars(Map.of("LOCALSTACK_HOST", "localhost"))
                .build());
        log.info("Create secret {} result: {} stderr: {}", secretName, execResult.getStdout(), execResult.getStderr());
        assertThat(execResult.getExitCode()).isZero();
    }

    @Test
    public void testSecretManagerIntegration_singleSecret() {
        SpringApplication application = new SpringApplication(Application.class);
        application.setWebApplicationType(WebApplicationType.NONE);

        try (ConfigurableApplicationContext context = runApplication(application,
                "aws-secretsmanager:secret1")) {
            assertThat(context.getEnvironment().getProperty("secret1"))
                    .isEqualTo("secret1Value");
        }
    }

    @Test
    public void testSecretManagerIntegration_twoSecrets() {
        SpringApplication application = new SpringApplication(Application.class);
        application.setWebApplicationType(WebApplicationType.NONE);

        try (ConfigurableApplicationContext context = runApplication(application,
                "aws-secretsmanager:secret1;secret2")) {
            assertThat(context.getEnvironment().getProperty("secret1"))
                    .isEqualTo("secret1Value");
            assertThat(context.getEnvironment().getProperty("secret2"))
                    .isEqualTo("secret2Value");
        }
    }

    @Test
    public void testSecretManagerIntegration_disabled() {
        SpringApplication application = new SpringApplication(Application.class);
        application.setWebApplicationType(WebApplicationType.NONE);

        try (ConfigurableApplicationContext context = runApplicationWithSecretsManagerDisabled(application,
                "aws-secretsmanager:secret1;secret2")) {
            assertThat(context.getEnvironment().getProperty("secret1"))
                    .isNull();
            assertThat(context.getEnvironment().getProperty("secret2"))
                    .isNull();
        }
    }

    @Test
    public void testSecretManagerIntegration_noImportedSecrets() {
        SpringApplication application = new SpringApplication(Application.class);
        application.setWebApplicationType(WebApplicationType.NONE);

        try (ConfigurableApplicationContext context = application.run()) {
            assertThat(context.getEnvironment().getProperty("secret1"))
                    .isNull();
        }
    }

    private ConfigurableApplicationContext runApplication(SpringApplication application, String springConfigImport) {
        return application.run("--spring.config.import=" + springConfigImport,
                "--jeap.aws.secretsmanager.region=" + localStackContainer.getRegion(),
                "--jeap.aws.secretsmanager.access-key-id=" + localStackContainer.getAccessKey(),
                "--jeap.aws.secretsmanager.secret-access-key=" + localStackContainer.getSecretKey(),
                "--jeap.aws.secretsmanager.endpoint-override=" + localStackContainer.getEndpointOverride(SECRETSMANAGER));
    }

    private ConfigurableApplicationContext runApplicationWithSecretsManagerDisabled(SpringApplication application, String springConfigImport) {
        return application.run("--spring.config.import=" + springConfigImport,
                "--jeap.aws.secretsmanager.enabled=false");
    }
}
