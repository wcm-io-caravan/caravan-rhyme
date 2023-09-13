package io.wcm.caravan.rhyme.spring.api;

import reactor.netty.http.client.HttpClient;

/**
 * Callback interface that can be used to customize a {@link reactor.netty.http.client.HttpClient}. E.g., to add a proxy configuration.
 */
@FunctionalInterface
public interface HttpClientCustomizer {

  /**
   * Callback to customize a {@link reactor.netty.http.client.HttpClient} instance.
   * @param httpClient the client to customize
   */
  void customize(HttpClient httpClient);

}
