package io.wcm.caravan.rhyme.spring.impl;

import java.io.ByteArrayInputStream;
import java.net.URI;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.client.HalResourceLoaderBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.api.spi.HttpClientCallback;
import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Component
public class WebClientHalResourceLoader implements HalResourceLoader {

	private final HalResourceLoader delegate;

	public WebClientHalResourceLoader() {
		this(true);
	}

	WebClientHalResourceLoader(boolean enableCaching) {

		WebClientSupport webClient = new WebClientSupport();

		HalResourceLoaderBuilder builder = HalResourceLoaderBuilder.create().withCustomHttpClient(webClient);
		if (enableCaching) {
			builder = builder.withMemoryCache();
		}

		delegate = builder.build();
	}

	@Override
	public Single<HalResponse> getHalResource(String uri) {

		return delegate.getHalResource(uri);
	}

	private final static class WebClientSupport implements HttpClientSupport {

		private final ConnectionProvider connectionProvider = ConnectionProvider
				.builder(WebClientHalResourceLoader.class.getSimpleName()).maxConnections(5000).build();

		private WebClient createWebClient() {

			HttpClient httpClient = HttpClient.create(connectionProvider);

			return WebClient.builder()//
					.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))//
					.clientConnector(new ReactorClientHttpConnector(httpClient))//
					.build();
		}

		@Override
		public void executeGetRequest(URI uri, HttpClientCallback callback) {

			WebClient client = createWebClient();

			client.get().uri(uri).retrieve()
					// any 200 responses will be parsed as a string and forwarded to the callback
					.toEntity(byte[].class).doOnSuccess(entity -> handleOkResponse(callback, entity))
					// any responses with error status should be handled specifically, as we want to
					// pass the status code and headers to the callback, and try to parse the body
					// as it may contain vnd.error information
					.onErrorResume(WebClientResponseException.class, ex -> handleErrorResponse(callback, ex))
					// any other exceptions thrown during the request or while handling the response
					// should be caught as well
					.doOnError(callback::onExceptionCaught)
					// finally subscribe so that the request is actually executed
					.subscribe();
		}

		private void handleOkResponse(HttpClientCallback callback, ResponseEntity<byte[]> entity) {

			callback.onHeadersAvailable(entity.getStatusCodeValue(), entity.getHeaders());

			callback.onBodyAvailable(new ByteArrayInputStream(entity.getBody()));
		}

		private Mono<ResponseEntity<byte[]>> handleErrorResponse(HttpClientCallback callback,
				WebClientResponseException ex) {

			callback.onHeadersAvailable(ex.getRawStatusCode(), ex.getHeaders());

			callback.onBodyAvailable(new ByteArrayInputStream(ex.getResponseBodyAsByteArray()));

			// since we have successfully called onBodyAvailable, all information from the
			// exception has been collected by the callback, and there is no need for
			// further error exception handling
			return Mono.empty();
		}
	}
}
