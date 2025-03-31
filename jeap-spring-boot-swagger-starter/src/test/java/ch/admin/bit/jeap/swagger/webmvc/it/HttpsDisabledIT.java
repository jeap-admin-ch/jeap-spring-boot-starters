package ch.admin.bit.jeap.swagger.webmvc.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TestConfiguration.class, properties = {"jeap.swagger.status=OPEN", "jeap.swagger.enforceServerBaseHttps=false"})
@AutoConfigureMockMvc
class HttpsDisabledIT {
    private final static String OPEN_API_SPEC_URL = "/api-docs/test";

    @Autowired
    private MockMvc mockMvc;

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
                .andExpect(jsonPath("$['servers'][0].url").value("http://someserver.com"));
    }
}
