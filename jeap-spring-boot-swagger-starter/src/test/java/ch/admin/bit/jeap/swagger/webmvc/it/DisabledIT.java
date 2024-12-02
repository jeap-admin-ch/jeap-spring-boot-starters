package ch.admin.bit.jeap.swagger.webmvc.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//Disabled is default, so no additional parameters need to be set
@SpringBootTest(classes = TestConfiguration.class)
@AutoConfigureMockMvc
class DisabledIT {
    private final static String SWAGGER_REDIRECT_URL = "/swagger-ui.html";
    private final static String SWAGGER_UI_URL = "/swagger-ui/index.html";
    private final static String SWAGGER_CONFIG_URL = "/api-docs/swagger-config";
    private final static String OPEN_API_SPEC_URL = "/api-docs/test";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void swaggerRedirectNotReachable() throws Exception {
        mockMvc.perform(get(SWAGGER_REDIRECT_URL))
                .andExpect(status().is(403));
    }

    @Test
    void swaggerUINotReachable() throws Exception {
        mockMvc.perform(get(SWAGGER_UI_URL))
                .andExpect(status().is(403));
    }

    @Test
    void swaggerConfigNotReachable() throws Exception {
        mockMvc.perform(get(SWAGGER_CONFIG_URL))
                .andExpect(status().is(403));
    }

    @Test
    void openApiNotReachable() throws Exception {
        mockMvc.perform(get(OPEN_API_SPEC_URL))
                .andExpect(status().is(403));
    }
}
