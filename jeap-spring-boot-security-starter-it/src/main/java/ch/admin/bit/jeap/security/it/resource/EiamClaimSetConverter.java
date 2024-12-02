package ch.admin.bit.jeap.security.it.resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Profile("eiam")
@Component("eiamClaimSetConverter")
public class EiamClaimSetConverter implements Converter<Map<String, Object>, Map<String, Object>> {

    // Converter for applying the spring security default mappings transforming some reserved claims
    final private Converter<Map<String, Object>, Map<String, Object>> defaultConverter = MappedJwtClaimSetConverter.withDefaults(Collections.emptyMap());

    @Override
    public Map<String, Object> convert(@NonNull Map<String, Object> claims) {
        Map<String, Object> mappedClaims = new HashMap<>(claims);
        mapClaim("role", "userroles", claims, mappedClaims);
        mapClaim("userExtId", "ext_id", claims, mappedClaims);
        mapLocale(claims, mappedClaims);
        mappedClaims.put("ctx", "USER");
        return defaultConverter.convert(mappedClaims);
    }

    private void mapClaim(String sourceClaimName, String targetClaimName, Map<String, Object> claims, Map<String, Object> mappedClaims) {
        Object sourceClaim = claims.get(sourceClaimName);
        if (sourceClaim != null) {
            mappedClaims.put(targetClaimName, sourceClaim);
        }
    }

    private void mapLocale(Map<String, Object> claims, Map<String, Object> mappedClaims) {
        Object language = claims.get("language");
        if (language != null) {
            mappedClaims.put("locale", language.toString().toUpperCase());
        }
    }

}
