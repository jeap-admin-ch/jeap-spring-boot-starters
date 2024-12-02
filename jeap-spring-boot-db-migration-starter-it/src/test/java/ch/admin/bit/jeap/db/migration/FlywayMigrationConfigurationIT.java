package ch.admin.bit.jeap.db.migration;

import ch.admin.bit.jeap.starter.db.config.ShutdownService;
import io.restassured.RestAssured;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
public class FlywayMigrationConfigurationIT {

    private static final String MIGRATED_SCHEMA_VERSION = "1.0.0";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            "postgres:15-alpine"
    );

    @MockBean
    private ShutdownService shutdownService;

    @Autowired
    private Flyway flyway;

    @LocalServerPort
    private Integer port;

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Nested
    class GivenACleanDatabaseAndWebContext {

        @BeforeEach
        void init() {
            RestAssured.baseURI = "http://localhost:" + port;
        }

        @AfterEach
        void cleanDatabase() {
            flyway.clean();
        }

        @Nested
        @TestPropertySource(properties = {"spring.main.cloud-platform=CLOUD_FOUNDRY"})
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        class WhenThePlattformIsNotKubernetes {

            @Test
            @Order(1)
            public void thenTheMigrationWasExecuted() {
                assertEquals(MIGRATED_SCHEMA_VERSION, flyway.info().getInfoResult().schemaVersion);
            }

            @Test
            @Order(2)
            public void thenTheServiceIsRunningAfterTheMigration() {
                given()
                        .when()
                        .get("/actuator/info")
                        .then()
                        .statusCode(200);
            }
        }

        @Nested
        @TestPropertySource(properties = {"spring.main.cloud-platform=KUBERNETES"})
        class WhenThePlattformIsKubernetes {

            @Nested
            @TestPropertySource(properties = {"database-migration.startup-migrate-mode-enabled=true"})
            @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
            class WhenStartupMigrationModeIsEnabled {

                @Test
                @Order(1)
                public void thenTheMigrationWasExecuted() {
                    assertEquals(MIGRATED_SCHEMA_VERSION, flyway.info().getInfoResult().schemaVersion);
                }

                @Test
                @Order(2)
                public void thenTheServiceIsRunningAfterTheMigration() {
                    given()
                            .when()
                            .get("/actuator/info")
                            .then()
                            .statusCode(200);
                }
            }

            @Nested
            @TestPropertySource(properties = {"database-migration.startup-migrate-mode-enabled=false"})
            class WhenStartupMigrationModeIsDisabled {

                @Nested
                @TestPropertySource(properties = {"database-migration.init-container=true"})
                @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
                class AndTheApplicationIsTheInitContainer {

                    @Test
                    @Order(1)
                    public void thenTheMigrationWasExecuted() {
                        assertEquals(MIGRATED_SCHEMA_VERSION, flyway.info().getInfoResult().schemaVersion);
                    }

                    @Test
                    @Order(2)
                    public void thenTheServiceIsShutDownAfterTheMigration() {
                        verify(shutdownService).shutdown(any(ApplicationContext.class), eq(0));
                    }
                }

                @Nested
                @TestPropertySource(properties = {"database-migration.init-container=false"})
                class AndTheApplicationIsNotTheInitContainer {

                    @Test
                    public void thenTheMigrationWasNotExecuted() {
                        assertNull(flyway.info().getInfoResult().schemaVersion);
                    }

                    @Test
                    public void thenTheServiceIsRunningAfterTheMigration() {
                        given()
                                .when()
                                .get("/actuator/info")
                                .then()
                                .statusCode(200);
                    }
                }
            }
        }
    }
}
