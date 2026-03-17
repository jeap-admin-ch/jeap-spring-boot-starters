package ch.admin.bit.jeap.security.resource.claimsetconverter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter;

import java.util.Map;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptyMap;

public abstract class AbstractClaimSetConverter implements Converter<Map<String, Object>, Map<String, Object>> {

    // Converter for applying the spring security default mappings transforming some reserved claims
    private final Converter<Map<String, Object>, Map<String, Object>> defaultConverter = MappedJwtClaimSetConverter.withDefaults(emptyMap());

    @Override
    public Map<String, Object> convert(@NonNull Map<String, Object> claims) {
        return defaultConverter.convert(doConvert(claims));
    }

    protected abstract Map<String, Object> doConvert(@NonNull Map<String, Object> claims);

    /**
     * Renames a claim from oldClaimName to newClaimName if a claim with oldClaimName is present in the claims map.
     *
     * @param oldClaimName the name of the claim to be renamed
     * @param newClaimName the new name of the claim
     * @param claims the claims map in which the claim should be renamed
     */
    protected void renameClaim(String oldClaimName, String newClaimName, Map<String, Object> claims) {
        Object claim = claims.get(oldClaimName);
        if (claim != null) {
            claims.put(newClaimName, claim);
            claims.remove(oldClaimName);
        }
    }

    /**
     * Maps a claim to a new claim by applying the given mapping function to the claim value if the claim is present in
     * the claims map. The original claim will be removed if the mapped claim name is different from the original claim name.
     *
     * @param originalClaimName the name of the claim to be mapped
     * @param mappedClaimName the name of the mapped claim
     * @param claims the claims map in which the claim should be mapped
     * @param mappingFunction the function to be applied to the claim value to get the mapped claim value
     */
    protected void mapClaim(String originalClaimName, String mappedClaimName, Map<String, Object> claims, UnaryOperator<Object> mappingFunction) {
        Object claim = claims.get(originalClaimName);
        if (claim != null) {
            Object mappedClaim = mappingFunction.apply(claim);
            claims.put(mappedClaimName, mappedClaim);
            if (!mappedClaimName.equals(originalClaimName)) {
                claims.remove(originalClaimName);
            }
        }
    }

}
