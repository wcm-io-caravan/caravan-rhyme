package io.wcm.caravan.rhyme.spring.impl;

import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Component
public class WebClientJsonResourceLoader implements HalResourceLoader {

  private static final Logger log = LoggerFactory.getLogger(WebClientJsonResourceLoader.class);

  private final ConnectionProvider connectionProvider = ConnectionProvider
      .builder(WebClientJsonResourceLoader.class.getSimpleName()).maxConnections(5000).build();

  @Override
  public Single<HalResponse> getHalResource(String requestUri) {

    if (log.isDebugEnabled()) {
      log.debug("Fetching resource from " + requestUri);
    }

    HttpClient httpClient = HttpClient.create(connectionProvider);

    WebClient client = WebClient.builder().codecs(this::applyCodecConfig)
        .clientConnector(new ReactorClientHttpConnector(httpClient)).build();

    URI parsedUri = parseUri(requestUri);

    ResponseSpec response = client.get().uri(parsedUri).retrieve();

    Mono<HalResponse> halResponse = response.toEntity(JsonNode.class).onErrorMap(ex -> remapException(ex, requestUri))
        .map(entity -> ensureAllLinksAreAbsolute(entity, requestUri))
        .map(WebClientJsonResourceLoader::toHalResponse);

    return Single.fromCompletionStage(halResponse.toFuture()).observeOn(Schedulers.computation());
  }

  private void applyCodecConfig(ClientCodecConfigurer configurer) {
    configurer
        .defaultCodecs()
        .maxInMemorySize(16 * 1024 * 1024);
  }

  private URI parseUri(String uriString) {

    try {
      return URI.create(uriString);
    }
    catch (IllegalArgumentException ex) {
      throw new HalApiDeveloperException(
          "The upstream URI " + uriString + " could not be parsed.", ex);
    }
  }

  private ResponseEntity<JsonNode> ensureAllLinksAreAbsolute(ResponseEntity<JsonNode> responseEntity, String requestUri) {

    URI parsedUri = URI.create(requestUri);
    String baseUri = StringUtils.substringBefore(requestUri, parsedUri.getPath());

    if (responseEntity.getBody() != null) {
      HalResource hal = new HalResource(responseEntity.getBody());
      recursivelyEnsureAllLinksAreAbsolute(hal, baseUri);
    }

    return responseEntity;
  }

  private void recursivelyEnsureAllLinksAreAbsolute(HalResource hal, String baseUri) {

    for (Link link : hal.getLinks().values()) {
      String href = link.getHref();
      if (href.startsWith("/")) {
        link.setHref(baseUri + href);
      }
    }

    for (HalResource embedded : hal.getEmbedded().values()) {
      recursivelyEnsureAllLinksAreAbsolute(embedded, baseUri);
    }
  }

  // FIXME: move to util class
  public static HalResponse toHalResponse(ResponseEntity<JsonNode> responseEntity) {

    HalResponse response = new HalResponse().withStatus(responseEntity.getStatusCodeValue());

    if (responseEntity.hasBody()) {
      response = response.withBody(responseEntity.getBody());
    }

    MediaType contentType = responseEntity.getHeaders().getContentType();
    if (contentType != null) {
      response = response.withContentType(contentType.toString());
    }

    String cacheControl = responseEntity.getHeaders().getCacheControl();
    Integer maxAge = CacheControlUtil.parseMaxAge(cacheControl);
    if (maxAge != null) {
      response = response.withMaxAge(maxAge);
    }

    return response;
  }

  private Throwable remapException(Throwable cause, String requestUrl) {

    String msg;
    Integer statusCode = null;

    if (cause instanceof WebClientResponseException) {
      statusCode = ((WebClientResponseException)cause).getRawStatusCode();
      msg = "Received unexpected status code " + statusCode + " when fetching upstream resource from "
          + requestUrl;
    }
    else if (cause instanceof UnsupportedMediaTypeException) {
      msg = "The resource retrieved from " + requestUrl + " is not a HAL+JSON resource";
    }
    else {
      msg = "Failed to retrieve any response from " + requestUrl;
    }

    return new HalApiClientException(msg, statusCode, requestUrl, cause);
  }

}
