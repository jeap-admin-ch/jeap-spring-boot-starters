package ch.admin.bit.jeap.security.client;

import java.util.Map;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

/**
 * Internalized from org.springframework.security.oauth2.server.resource.web.reactive.function.client.ServletBearerExchangeFilterFunction
 * to not depend on the security-oauth2-resource-server module.
 * <p>
 * An {@link ExchangeFilterFunction} that adds the
 * <a href="https://tools.ietf.org/html/rfc6750#section-1.2" target="_blank">Bearer
 * Token</a> from an existing {@link OAuth2Token} tied to the current
 * {@link Authentication}.
 * <p>
 * Suitable for Servlet applications, applying it to a typical
 * {@link org.springframework.web.reactive.function.client.WebClient} configuration:
 *
 * <pre>
 *
 *  &#64;Bean
 *  WebClient webClient() {
 *      ServletBearerExchangeFilterFunction bearer = new ServletBearerExchangeFilterFunction();
 *      return WebClient.builder()
 *              .filter(bearer).build();
 *  }
 * </pre>
 * <p>
 * To locate the bearer token, this looks in the Reactor {@link Context} for a key of type
 * {@link Authentication}.
 *
 * @author Josh Cummings
 * @since 5.2
 */
public final class ServletBearerExchangeFilterFunction implements ExchangeFilterFunction {

	static final String SECURITY_REACTOR_CONTEXT_ATTRIBUTES_KEY = "org.springframework.security.SECURITY_CONTEXT_ATTRIBUTES";

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		// @formatter:off
		return oauth2Token().map((token) -> bearer(request, token))
				.defaultIfEmpty(request)
				.flatMap(next::exchange);
		// @formatter:on
	}

	private Mono<OAuth2Token> oauth2Token() {
		// @formatter:off
		return Mono.deferContextual(Mono::just)
				.cast(Context.class)
				.flatMap(this::currentAuthentication)
				.filter((authentication) -> authentication.getCredentials() instanceof OAuth2Token)
				.map(Authentication::getCredentials)
				.cast(OAuth2Token.class);
		// @formatter:on
	}

	private Mono<Authentication> currentAuthentication(Context ctx) {
		return Mono.justOrEmpty(getAttribute(ctx, Authentication.class));
	}

	private <T> T getAttribute(Context ctx, Class<T> clazz) {
		// NOTE: SecurityReactorContextConfiguration.SecurityReactorContextSubscriber adds
		// this key
		if (!ctx.hasKey(SECURITY_REACTOR_CONTEXT_ATTRIBUTES_KEY)) {
			return null;
		}
		Map<Class<T>, T> attributes = ctx.get(SECURITY_REACTOR_CONTEXT_ATTRIBUTES_KEY);
		return attributes.get(clazz);
	}

	private ClientRequest bearer(ClientRequest request, OAuth2Token token) {
		// @formatter:off
		return ClientRequest.from(request)
				.headers((headers) -> headers.setBearerAuth(token.getTokenValue()))
				.build();
		// @formatter:on
	}

}
