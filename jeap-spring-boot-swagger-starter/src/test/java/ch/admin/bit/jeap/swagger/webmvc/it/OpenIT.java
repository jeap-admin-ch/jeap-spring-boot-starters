package ch.admin.bit.jeap.swagger.webmvc.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TestConfiguration.class, properties = {"jeap.swagger.status=OPEN"})
@AutoConfigureMockMvc
class OpenIT {
    private final static String SWAGGER_REDIRECT_URL = "/swagger-ui.html";
    private final static String SWAGGER_UI_URL = "/swagger-ui/index.html";
    private final static String SWAGGER_CONFIG_URL = "/api-docs/swagger-config";
    private final static String OPEN_API_SPEC_URL = "/api-docs/test";
    private final static String OAUTH_REDIRECT_URL = "http://localhost/swagger-ui/oauth2-redirect.html";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void swaggerRedirect() throws Exception {
        mockMvc.perform(get(SWAGGER_REDIRECT_URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", SWAGGER_UI_URL));
    }

    @Test
    void swaggerConfigUrl() throws Exception {
        mockMvc.perform(get(SWAGGER_CONFIG_URL))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$['configUrl']").value(SWAGGER_CONFIG_URL));
    }

    @Test
    void swaggerUIReachable() throws Exception {
        mockMvc.perform(get(SWAGGER_UI_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html"));
    }

    @Test
    void swaggerConfig() throws Exception {
        mockMvc.perform(get(SWAGGER_CONFIG_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$['configUrl']").value(SWAGGER_CONFIG_URL))
                .andExpect(jsonPath("$['oauth2RedirectUrl']").value(OAUTH_REDIRECT_URL))
                .andExpect(jsonPath("$['urls'][0]['url']").value(OPEN_API_SPEC_URL))
                .andExpect(jsonPath("$['urls'][0]['name']").value("test"));
    }

    @Test
    void openApi() throws Exception {
        mockMvc.perform(get(OPEN_API_SPEC_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$['openapi']").value("3.1.0"))
                .andExpect(jsonPath("$['servers'][0].url").value("http://localhost"));
    }

    @Test
    void openApiServerBaseUrlHttpsOnNonLocalhost() throws Exception {
        mockMvc.perform(get(OPEN_API_SPEC_URL)
                        .with(req -> {
                            req.setServerName("someserver.com");
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$['servers'][0].url").value("https://someserver.com"));
    }
}
