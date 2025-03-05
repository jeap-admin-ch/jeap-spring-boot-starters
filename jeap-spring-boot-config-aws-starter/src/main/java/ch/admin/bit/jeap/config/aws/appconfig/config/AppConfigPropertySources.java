package ch.admin.bit.jeap.config.aws.appconfig.config;

import ch.admin.bit.jeap.config.aws.appconfig.client.JeapAppConfigDataClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

public class AppConfigPropertySources {

	/**
	 * Logger will be post-processed and cannot be final
	 */
	@SuppressWarnings("FieldMayBeFinal")
	private static Log log = LogFactory.getLog(AppConfigPropertySources.class);

	@Nullable
	public AppConfigPropertySource<JeapAppConfigDataClient> createPropertySource(String name, boolean optional, JeapAppConfigDataClient client) {
		Assert.notNull(name, "name is required");
		Assert.notNull(client, "client is required");

		log.info("Adding property source for AppConfig with name: '" + name + "', optional: " + optional);
		try {
			AppConfigPropertySource<JeapAppConfigDataClient> propertySource = new AppConfigPropertySource<>(name, client);
            propertySource.init();
			return propertySource;
		}
		catch (Exception e) {
			if (!optional) {
				throw new AppConfigPropertySourceNotFoundException(name, e);
			}
			else {
				log.info("Unable to load optional AppConfig location for property source '" + name + "'. " + e.getMessage());
			}
		}

		return null;
	}

	static class AppConfigPropertySourceNotFoundException extends RuntimeException {

		AppConfigPropertySourceNotFoundException(String context, Exception source) {
			super("Error loading PropertySource for context '" + context + "'", source);
		}

	}

}
