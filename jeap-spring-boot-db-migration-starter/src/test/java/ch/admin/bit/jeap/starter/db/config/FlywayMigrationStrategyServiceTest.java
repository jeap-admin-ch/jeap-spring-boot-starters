package ch.admin.bit.jeap.starter.db.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.output.InfoOutput;
import org.flywaydb.core.api.output.InfoResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlywayMigrationStrategyServiceTest {

    private static final int SHUTDOWN_STATUS_NORMAL = 0;
    private static final int SHUTDOWN_STATUS_ABNORMAL = 1;

    private static final String MIGRATION_STATE_PENDING = "PENDING";
    private static final String MIGRATION_STATE_SUCCEEDED = "SUCCEEDED";
    private static final String MIGRATION_STATE_FAILED = "FAILED";

    ApplicationContext ctx = mock(ApplicationContext.class);
    Flyway flyway = mock(Flyway.class);

    @Mock
    ShutdownService shutdownService;

    @InjectMocks
    FlywayMigrationStrategyService cut;

    private MigrationInfoService getMigrationInfoServiceMockWithSingletonMigrationState(String migrationState) {
        //The first migration is always the flyway migration table
        InfoOutput firstMigrationInfo = mock(InfoOutput.class);
        firstMigrationInfo.category = "VERSIONED";
        firstMigrationInfo.state = MIGRATION_STATE_SUCCEEDED;

        InfoOutput migrationInfo = mock(InfoOutput.class);
        migrationInfo.category = "VERSIONED";
        migrationInfo.state = migrationState;

        List<InfoOutput> infoOutputList = Arrays.asList(firstMigrationInfo, migrationInfo);
        InfoResult infoResult = mock(InfoResult.class);
        infoResult.migrations = infoOutputList;

        MigrationInfoService infoService = mock(MigrationInfoService.class);
        when(infoService.getInfoResult()).thenReturn(infoResult);
        when(infoService.all()).thenReturn(new MigrationInfo[0]);

        return infoService;
    }

    @Nested
    class GivenTestContext {

        @Nested
        class WhenExecuteStartupModeStrategyIsCalled {

            @BeforeEach
            void init() {
                cut.executeStartupModeStrategy(flyway);
            }

            @Test
            void thenFlywayShouldMigrate() {
                verify(flyway).migrate();
            }

            @Test
            void thenTheServiceShouldNotBeShutDown() {
                verifyNoInteractions(shutdownService);
            }
        }

        @Nested
        class WhenExecuteInitContainerStrategyIsCalled {

            @BeforeEach
            void init() {
                cut.executeInitContainerStrategy(flyway);
            }

            @Test
            void thenFlywayShouldMigrate() {
                verify(flyway).migrate();
            }
        }

        @Nested
        class WhenExecuteApplicationContainerStrategyIsCalled {

            @Nested
            class AndAllMigrationsWereSuccessful {

                @BeforeEach
                void init() {
                    MigrationInfoService infoService = getMigrationInfoServiceMockWithSingletonMigrationState(MIGRATION_STATE_SUCCEEDED);
                    when(flyway.info()).thenReturn(infoService);

                    cut.executeApplicationContainerStrategy(ctx, flyway);
                }

                @Test
                void thenNoFurtherMigrationsAreNeededAndTheServiceStaysUp() {
                    verifyNoInteractions(shutdownService);
                }
            }

            @Nested
            class AndOneMigrationFailed {

                @BeforeEach
                void init() {
                    MigrationInfoService infoService = getMigrationInfoServiceMockWithSingletonMigrationState(MIGRATION_STATE_FAILED);
                    when(flyway.info()).thenReturn(infoService);

                    cut.executeApplicationContainerStrategy(ctx, flyway);
                }

                @Test
                void thenFurtherMigrationsAreNeededAndTheServiceIsShutDown() {
                    verify(shutdownService).shutdown(ctx, SHUTDOWN_STATUS_ABNORMAL);
                }
            }

            @Nested
            class AndOneMigrationIsPending {

                @BeforeEach
                void init() {
                    MigrationInfoService infoService = getMigrationInfoServiceMockWithSingletonMigrationState(MIGRATION_STATE_PENDING);
                    when(flyway.info()).thenReturn(infoService);

                    cut.executeApplicationContainerStrategy(ctx, flyway);
                }

                @Test
                void thenFurtherMigrationsAreNeededAndTheServiceIsShutDown() {
                    verify(shutdownService).shutdown(ctx, SHUTDOWN_STATUS_ABNORMAL);
                }
            }
        }
    }
}

