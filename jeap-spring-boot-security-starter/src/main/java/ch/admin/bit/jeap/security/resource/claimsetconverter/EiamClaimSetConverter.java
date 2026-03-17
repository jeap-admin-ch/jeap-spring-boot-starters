package ch.admin.bit.jeap.security.resource.claimsetconverter;

import org.springframework.lang.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Claim set converter implementation adapting the claims of an access token issued by eIAM to jEAP security.
 */
public class EiamClaimSetConverter extends AbstractClaimSetConverter {

    @Override
    public Map<String, Object> doConvert(@NonNull Map<String, Object> claims) {
        Map<String, Object> mappedClaims = new HashMap<>(claims);
        renameClaim("role", "userroles", mappedClaims);
        renameClaim("userExtId", "ext_id", mappedClaims);
        mapClaim("language", "locale", mappedClaims,
                language -> language.toString().toUpperCase());
        mappedClaims.put("ctx", "USER"); // eIAM only manages users (no systems)
        return mappedClaims;
    }

}
