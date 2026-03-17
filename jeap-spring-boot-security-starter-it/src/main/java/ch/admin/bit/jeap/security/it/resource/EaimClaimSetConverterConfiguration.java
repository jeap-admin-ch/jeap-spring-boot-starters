package ch.admin.bit.jeap.security.it.resource;

import ch.admin.bit.jeap.security.resource.claimsetconverter.EiamClaimSetConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EaimClaimSetConverterConfiguration {

    public static final String CONVERTER_BEAN_NAME = "eiamClaimSetConverter";

    @Bean(CONVERTER_BEAN_NAME)
    EiamClaimSetConverter eiamClaimSetConverter() {
        return new EiamClaimSetConverter();
    }

}
