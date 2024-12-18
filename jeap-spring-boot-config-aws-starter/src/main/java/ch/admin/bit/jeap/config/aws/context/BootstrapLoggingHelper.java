package ch.admin.bit.jeap.config.aws.context;

import lombok.experimental.UtilityClass;
import org.apache.commons.logging.Log;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * This class configures the deferred log factory for the configured loggers
 * Note: This class is based on <a href="https://github.com/awspring/spring-cloud-aws">Spring Cloud AWS</a>,
 * which is licensed under the Apache License 2.0.
 */
@UtilityClass
public final class BootstrapLoggingHelper {

	public static void reconfigureLoggers(DeferredLogFactory logFactory, String... classes) {
		// loggers in these classes must be static non-final
		List<Class<?>> loggers = new ArrayList<>();
		for (String clazz : classes) {
			if (ClassUtils.isPresent(clazz, null)) {
				try {
					loggers.add(Class.forName(clazz));
				}
				catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		}

		loggers.forEach(it -> reconfigureLogger(it, logFactory));
	}

	private static void reconfigureLogger(Class<?> type, DeferredLogFactory logFactory) {
		ReflectionUtils.doWithFields(type, field -> {
			field.setAccessible(true);
			field.set(null, logFactory.getLog(type));

		}, BootstrapLoggingHelper::isUpdatableLogField);
	}

	private static boolean isUpdatableLogField(Field field) {
		return !Modifier.isFinal(field.getModifiers()) && field.getType().isAssignableFrom(Log.class);
	}

}
