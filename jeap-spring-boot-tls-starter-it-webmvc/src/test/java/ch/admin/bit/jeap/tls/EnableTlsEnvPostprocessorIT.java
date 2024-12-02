package ch.admin.bit.jeap.tls;

import lombok.SneakyThrows;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import javax.net.ssl.SSLContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TestApplication.class)
class EnableTlsEnvPostprocessorIT {

    @LocalServerPort
    private int randomServerPort;

    @Test
    void testHttpFails() {
        assertThatThrownBy(() -> getRestClient("http").get().retrieve().body(String.class))
                .isInstanceOf(RestClientException.class)
                // TLS needed
                .hasMessageContaining("This combination of host and port requires TLS");
    }

    @Test
    void testHttpsSucceeds() {
        String response = getRestClient("https").get().retrieve().body(String.class);
        assertThat(response).isEqualTo("Hello World!");
    }


    private RestClient getRestClient(String scheme) {
        String baseUrl = UriComponentsBuilder.newInstance()
                .scheme(scheme)
                .host("localhost")
                .path("/hello")
                .port(randomServerPort)
                .build()
                .toUriString();
        return RestClient.builder()
                .requestFactory(createRequestFactoryAcceptingSelfSignedCertificates())
                .baseUrl(baseUrl)
                .build();
    }

    @SneakyThrows
    private ClientHttpRequestFactory createRequestFactoryAcceptingSelfSignedCertificates() {
        TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
                .register("https", sslsf)
                .register("http", new PlainConnectionSocketFactory())
                .build();
        BasicHttpClientConnectionManager connectionManager =
                new BasicHttpClientConnectionManager(socketFactoryRegistry);
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
        return  new HttpComponentsClientHttpRequestFactory(httpClient);
    }

}
