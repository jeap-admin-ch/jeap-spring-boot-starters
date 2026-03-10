package ch.admin.bit.jeap.security.resource.validation;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DecoderCreatorTest {

    private static final int TIMEOUT_MS = 500;
    private static final JeapJwtDecoderFactory.JwksTimeoutConfiguration TIMEOUT_CONFIGURATION = new JeapJwtDecoderFactory.JwksTimeoutConfiguration(TIMEOUT_MS, TIMEOUT_MS); // we take the same for both

    private WireMockServer wireMockServer;


    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        wireMockServer.stubFor(get(urlEqualTo("/.well-known/jwks.json"))
                .willReturn(aResponse()
                        .withFixedDelay(5000 + TIMEOUT_MS)
                        .withBody("{\"keys\": []}") // valid but empty JWK set
                )
        );
    }

    @AfterEach
    void stopWireMock() {
        wireMockServer.stop();
    }

    @Test
    void createServletDecoder_withDelay() {
        OAuth2TokenValidator<Jwt> jwtValidator = mock(OAuth2TokenValidator.class);
        Converter<Map<String, Object>, Map<String, Object>> claimSetConverter = source -> Map.of();
        String jwkSetUri = "http://localhost:8089/.well-known/jwks.json";
        JwtDecoder jwtDecoder = JeapJwtDecoderFactory.DecoderCreator.createServletDecoder(jwkSetUri, jwtValidator, claimSetConverter, TIMEOUT_CONFIGURATION);

        Exception exception = assertThrows(Exception.class, () -> jwtDecoder.decode("eyJhbGciOiAiUlM1MTIiLCAidHlwIjogIkpXVCJ9.eyJzdWIiOiAiMTIzNDU2Nzg5MCJ9.c2lnbmF0dXJl"));
        assertTrue(exception.getMessage().contains("timed out"), "Got: " + exception.getMessage());

    }

    @Test
    void createServletDecoder_withUnreachableEndpoint() {
        OAuth2TokenValidator<Jwt> jwtValidator = mock(OAuth2TokenValidator.class);
        Converter<Map<String, Object>, Map<String, Object>> claimSetConverter = source -> Map.of();
        String jwkSetUri = "http://10.255.255.1/.well-known/jwks.json"; // unreachable private network
        JwtDecoder jwtDecoder = JeapJwtDecoderFactory.DecoderCreator.createServletDecoder(jwkSetUri, jwtValidator, claimSetConverter, TIMEOUT_CONFIGURATION);

        assertThrows(Exception.class, () -> jwtDecoder.decode("eyJhbGciOiAiUlM1MTIiLCAidHlwIjogIkpXVCJ9.eyJzdWIiOiAiMTIzNDU2Nzg5MCJ9.c2lnbmF0dXJl"));
    }
}
