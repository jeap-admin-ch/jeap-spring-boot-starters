package ch.admin.bit.jeap.tls;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.netty.http.client.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TestApplication.class)
class EnableTlsEnvPostprocessorIT {

    @LocalServerPort
    private int randomServerPort;

    @Test
    void testHttpFails() {
        assertThatThrownBy(() -> createWebClient("http").get().retrieve().bodyToMono(String.class).block())
                .isInstanceOf(WebClientException.class)
                // connection cannot be established because TLS is needed
                .hasMessageContaining("Connection prematurely closed BEFORE response");
    }

    @Test
    void testHttpsSucceeds() {
        String response = createWebClient("https").get().retrieve().bodyToMono(String.class).block();
        assertThat(response).isEqualTo("Hello World!");
    }

    private WebClient createWebClient(String scheme) {
        String baseUrl = UriComponentsBuilder.newInstance()
                .scheme(scheme)
                .host("localhost")
                .path("/hello")
                .port(randomServerPort)
                .build()
                .toUriString();
        return WebClient.builder()
                .clientConnector(createClientHttpConnectorAcceptingSelfSignedCertificates())
                .baseUrl(baseUrl)
                .build();
    }

    @SneakyThrows
    private ClientHttpConnector createClientHttpConnectorAcceptingSelfSignedCertificates() {
        SslContext context = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();
        HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(context));
        return new ReactorClientHttpConnector(httpClient);
    }

}
