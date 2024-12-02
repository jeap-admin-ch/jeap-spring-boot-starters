package ch.admin.bit.jeap.security.it.resource;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Mono;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class BearerTokenResource {

    public static final String API_PATH = "/api/bearertoken";

    private static final Pattern BEARER_AUTH_PATTERN = Pattern.compile("[Bb]earer (.*)");

    @GetMapping(API_PATH)
    public Mono<String> getBearerAuthToken(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        if (StringUtils.isEmpty(authorization)) {
            return Mono.just("");
        }

        Matcher matcher = BEARER_AUTH_PATTERN.matcher(authorization);
        if (matcher.matches()) {
            return Mono.just(matcher.group(1));
        }
        else {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Not bearer authorization.");
        }
    }

}
