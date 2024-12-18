package ch.admin.bit.jeap.config.aws.secretsmanager.config;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.boot.context.config.ConfigDataResource;

@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Value
public class AwsSecretsManagerConfigDataResource extends ConfigDataResource {

    @EqualsAndHashCode.Include
    String secretName;
    @EqualsAndHashCode.Include
    boolean optional;
    @EqualsAndHashCode.Include
    boolean enabled;

    AwsSecretsManagerPropertySources propertySources;
}
