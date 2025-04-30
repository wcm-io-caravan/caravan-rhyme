package io.wcm.caravan.rhyme.spring.api;

import java.net.URI;

import org.springframework.web.reactive.function.client.WebClient;

/**
 * Callback interface to provide an instance of {@link WebClient} for the given uri.
 */
@FunctionalInterface
public interface WebClientProvider {

  /**
   * Returns an instance of {@link WebClient} for the given uri.
   */
  WebClient webClientForUri(URI uri);

}
