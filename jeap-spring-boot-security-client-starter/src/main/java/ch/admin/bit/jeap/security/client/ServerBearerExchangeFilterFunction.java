package ch.admin.bit.jeap.security.client;

import reactor.core.publisher.Mono;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

/**
 *  Internalized from org.springframework.security.oauth2.server.resource.web.reactive.function.client.ServerBearerExchangeFilterFunction
 *  to not depend on the security-oauth2-resource-server module.
 * <p>
 * An {@link ExchangeFilterFunction} that adds the
 * <a href="https://tools.ietf.org/html/rfc6750#section-1.2" target="_blank">Bearer
 * Token</a> from an existing {@link OAuth2Token} tied to the current
 * {@link Authentication}.
 * <p>
 * Suitable for Reactive applications, applying it to a typical
 * {@link org.springframework.web.reactive.function.client.WebClient} configuration:
 *
 * <pre>
 *
 *  &#64;Bean
 *  WebClient webClient() {
 *      ServerBearerExchangeFilterFunction bearer = new ServerBearerExchangeFilterFunction();
 *      return WebClient.builder()
 *              .filter(bearer).build();
 *  }
 * </pre>
 *
 * @author Josh Cummings
 * @since 5.2
 */
public final class ServerBearerExchangeFilterFunction implements ExchangeFilterFunction {

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
		return currentAuthentication()
				.filter((authentication) -> authentication.getCredentials() instanceof OAuth2Token)
				.map(Authentication::getCredentials)
				.cast(OAuth2Token.class);
		// @formatter:on
	}

	private Mono<Authentication> currentAuthentication() {
		// @formatter:off
		return ReactiveSecurityContextHolder.getContext()
				.map(SecurityContext::getAuthentication);
		// @formatter:on
	}

	private ClientRequest bearer(ClientRequest request, OAuth2Token token) {
		// @formatter:off
		return ClientRequest.from(request)
				.headers((headers) -> headers.setBearerAuth(token.getTokenValue()))
				.build();
		// @formatter:on
	}

}
