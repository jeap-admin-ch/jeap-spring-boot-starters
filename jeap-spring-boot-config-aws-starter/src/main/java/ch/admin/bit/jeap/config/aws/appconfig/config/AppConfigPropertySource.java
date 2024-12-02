package ch.admin.bit.jeap.config.aws.appconfig.config;

import ch.admin.bit.jeap.config.aws.appconfig.client.JeapAppConfigDataClient;
import lombok.EqualsAndHashCode;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
public class AppConfigPropertySource<T extends JeapAppConfigDataClient> extends EnumerablePropertySource<T> {

	private final Map<String, Object> properties = new LinkedHashMap<>();

	public AppConfigPropertySource(String name, T source) {
		super(name, source);
	}

	public void init() {
		this.source.getProperties().forEach((property, value) -> this.properties.put((String) property, value));
	}

	@Override
	public @NonNull String[] getPropertyNames() {
		return this.properties.keySet().toArray(new String[0]);
	}

	@Override
	@Nullable
	public Object getProperty(@NonNull String name) {
		return this.properties.get(name);
	}
}
