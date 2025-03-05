package ch.admin.bit.jeap.config.aws.appconfig.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationRequest;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationResponse;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionResponse;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JeapAppConfigDataClientPollingDisabledTest {

    @Mock
    private AppConfigDataClient appConfigDataClient;

    @Mock
    private StartConfigurationSessionResponse startConfigurationSessionResponse;

    @Mock
    private GetLatestConfigurationResponse getLatestConfigurationResponse;

    @Captor
    ArgumentCaptor<GetLatestConfigurationRequest> getLatestConfigurationRequestCaptor;

    private JeapAppConfigDataClient jeapAppConfigDataClient;

    @BeforeEach
    void setup() {
        when(appConfigDataClient.startConfigurationSession(any(StartConfigurationSessionRequest.class))).thenReturn(startConfigurationSessionResponse);
        when(appConfigDataClient.getLatestConfiguration(any(GetLatestConfigurationRequest.class))).thenReturn(getLatestConfigurationResponse);
        jeapAppConfigDataClient = new JeapAppConfigDataClient(appConfigDataClient, "appId", "envId", "profileId", false);
    }

    @Test
    void retrieveCurrentConfiguration_newConfigurationRetrieved() {
        when(getLatestConfigurationResponse.configuration())
                .thenReturn(SdkBytes.fromUtf8String("{\"key\":\"value\"}"))
                .thenReturn(SdkBytes.fromUtf8String("{\"key\":\"new value\"}"));
        when(getLatestConfigurationResponse.contentType()).thenReturn("application/json");

        jeapAppConfigDataClient.retrieveCurrentConfiguration();
        assertThat(jeapAppConfigDataClient.getProperties()).containsEntry("key", "value");

        jeapAppConfigDataClient.retrieveCurrentConfiguration();
        assertThat(jeapAppConfigDataClient.getProperties()).containsEntry("key", "new value");

    }

    @Test
    void retrieveCurrentConfiguration_noNewConfiguration() {
        when(getLatestConfigurationResponse.configuration())
                .thenReturn(SdkBytes.fromUtf8String("{\"key\":\"value\"}"))
                .thenReturn(SdkBytes.fromUtf8String(""));
        when(getLatestConfigurationResponse.contentType()).thenReturn("application/json");

        jeapAppConfigDataClient.retrieveCurrentConfiguration();
        assertThat(jeapAppConfigDataClient.getProperties()).containsEntry("key", "value");

        jeapAppConfigDataClient.retrieveCurrentConfiguration();
        assertThat(jeapAppConfigDataClient.getProperties()).containsEntry("key", "value");

    }

    @Test
    void retrieveCurrentConfiguration_useNewToken() {
        when(getLatestConfigurationResponse.configuration()).thenReturn(SdkBytes.fromUtf8String(""));

        when(startConfigurationSessionResponse.initialConfigurationToken()).thenReturn("initialToken");

        when(getLatestConfigurationResponse.nextPollConfigurationToken())
                .thenReturn("first")
                .thenReturn("second");

        jeapAppConfigDataClient.retrieveCurrentConfiguration();
        jeapAppConfigDataClient.retrieveCurrentConfiguration();
        jeapAppConfigDataClient.retrieveCurrentConfiguration();

        verify(appConfigDataClient, times(3)).getLatestConfiguration(getLatestConfigurationRequestCaptor.capture());
        List<GetLatestConfigurationRequest> values = getLatestConfigurationRequestCaptor.getAllValues();
        assertThat(values.get(0).configurationToken()).isEqualTo("initialToken");
        assertThat(values.get(1).configurationToken()).isEqualTo("first");
        assertThat(values.get(2).configurationToken()).isEqualTo("second");

    }

    @ParameterizedTest
    @MethodSource("provideConfigurationInputs")
    void retrieveCurrentConfiguration_parseConfiguration(String contentType, String configuration) {
        when(getLatestConfigurationResponse.configuration()).thenReturn(SdkBytes.fromUtf8String(configuration));
        when(getLatestConfigurationResponse.contentType()).thenReturn(contentType);

        jeapAppConfigDataClient.retrieveCurrentConfiguration();
        assertThat(jeapAppConfigDataClient.getProperties()).containsEntry("jeap.swagger.status", "OPEN");
        assertThat(jeapAppConfigDataClient.getProperties()).containsEntry("jeap.security.oauth2.resourceserver.system-name", "jme");
        assertThat(jeapAppConfigDataClient.getProperties()).containsEntry("spring.main.banner-mode", false);
        assertThat(jeapAppConfigDataClient.getProperties()).containsEntry("spring.test", true);
    }

    @Test
    void retrieveCurrentConfiguration_parseTextConfiguration() {
        when(getLatestConfigurationResponse.configuration()).thenReturn(SdkBytes.fromUtf8String(
                """
                        jeap.swagger.status=OPEN
                        jeap.security.oauth2.resourceserver.system-name=jme
                        spring.main.banner-mode=false
                        spring.test=true
                        """
        ));
        when(getLatestConfigurationResponse.contentType()).thenReturn("text/plain");

        jeapAppConfigDataClient.retrieveCurrentConfiguration();
        assertThat(jeapAppConfigDataClient.getProperties()).containsEntry("jeap.swagger.status", "OPEN");
        assertThat(jeapAppConfigDataClient.getProperties()).containsEntry("jeap.security.oauth2.resourceserver.system-name", "jme");
        assertThat(jeapAppConfigDataClient.getProperties()).containsEntry("spring.main.banner-mode", "false");
        assertThat(jeapAppConfigDataClient.getProperties()).containsEntry("spring.test", "true");
    }

    private static Stream<Arguments> provideConfigurationInputs() {
        return Stream.of(
                Arguments.of("application/x-yaml",
                        """
                                jeap:
                                  swagger:
                                    status: OPEN
                                  security:
                                    oauth2:
                                      resourceserver:
                                        system-name: "jme"
                                                        
                                spring:
                                  test:
                                    true
                                  main:
                                    banner-mode: false
                                """),
                Arguments.of("application/json",
                        """
                                {
                                "jeap.swagger.status": "OPEN",
                                "jeap.security.oauth2.resourceserver.system-name":"jme",
                                "spring.main.banner-mode": false,
                                "spring.test": true
                                }
                                """)
        );
    }


}
