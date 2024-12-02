package ch.admin.bit.jeap.config.aws.appconfig.config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.boot.context.config.ConfigDataResource;

@Getter
@ToString
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
public class AppConfigDataResource extends ConfigDataResource {

	@EqualsAndHashCode.Include
	private final String appId;

	@EqualsAndHashCode.Include
	private final String profileId;

	@EqualsAndHashCode.Include
	private final boolean optional;

	private final AppConfigPropertySources propertySources;

}
