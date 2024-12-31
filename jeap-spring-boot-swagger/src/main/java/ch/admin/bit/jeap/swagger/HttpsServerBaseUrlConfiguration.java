package ch.admin.bit.jeap.swagger;

import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.ServerBaseUrlCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpRequest;
import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;

@Slf4j
@AutoConfiguration
@ConditionalOnProperty(name = "jeap.swagger.enforceServerBaseHttps", matchIfMissing = true, havingValue = "true")
public class HttpsServerBaseUrlConfiguration {

    @Bean
    HttpsEnforcerServerBaseUrlCustomizer httpsEnforcerServerBaseUrlCustomizer() {
        return new HttpsEnforcerServerBaseUrlCustomizer();
    }

    static class HttpsEnforcerServerBaseUrlCustomizer implements ServerBaseUrlCustomizer {

        @Override
        public String customize(String serverBaseUrl, HttpRequest request) {
            try {
                if (StringUtils.hasText(serverBaseUrl)) {
                    URL url = new URL(serverBaseUrl);
                    if (url.getProtocol().equalsIgnoreCase("http") && !url.getHost().equalsIgnoreCase("localhost")) {
                        return "https" + serverBaseUrl.substring(4);
                    }
                }
                return serverBaseUrl;
            } catch (MalformedURLException e) {
                log.warn("Could not customize swagger base server url: " + serverBaseUrl, e);
                return serverBaseUrl;
            }
        }

    }

}
