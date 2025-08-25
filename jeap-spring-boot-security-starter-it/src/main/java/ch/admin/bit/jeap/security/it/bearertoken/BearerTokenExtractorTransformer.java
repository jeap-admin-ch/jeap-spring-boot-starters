package ch.admin.bit.jeap.security.it.bearertoken;

import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.springframework.http.HttpHeaders;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BearerTokenExtractorTransformer implements ResponseTransformerV2 {

    public static final String NAME = "bearer-token-extractor";

    private static final Pattern BEARER_AUTH_PATTERN = Pattern.compile("[Bb]earer (.*)");

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }

    @Override
    public Response transform(Response response, ServeEvent serveEvent) {
        HttpHeader authorizationHeader = serveEvent.getRequest().getHeaders().getHeader(HttpHeaders.AUTHORIZATION);
        if (!authorizationHeader.isPresent() ) {
            return Response.response().status(200).body("").build();
        }
        String authorization = authorizationHeader.firstValue();
        Matcher matcher = BEARER_AUTH_PATTERN.matcher(authorization);
        if (matcher.matches()) {
            return Response.response().status(200).body(matcher.group(1)).build();
        } else {
            return Response.response().status(400).body("{\"error\": \"Not bearer authorization.\"}").build();
        }
    }
}
