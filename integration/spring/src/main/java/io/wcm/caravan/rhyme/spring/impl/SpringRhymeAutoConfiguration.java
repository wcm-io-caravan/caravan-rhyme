/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2021 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.caravan.rhyme.spring.impl;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.wcm.caravan.rhyme.api.client.HalResourceLoaderBuilder;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;
import io.wcm.caravan.rhyme.spring.api.HttpClientCustomizer;
import io.wcm.caravan.rhyme.spring.api.SpringRhyme;
import io.wcm.caravan.rhyme.spring.api.WebClientProvider;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

/**
 * Registers all components from the impl package, and provides default implementations for spring beans required by
 * {@link SpringRhyme}.
 * You can override any of these bean definitions if you need a different implementation in your project (or tests)
 */
@Configuration
@ComponentScan(basePackages = "io.wcm.caravan.rhyme.spring.impl")
class SpringRhymeAutoConfiguration {

  /**
   * Defines the default resource loader used by the {@link SpringRhyme} implementation class: a
   * {@link HalResourceLoader} that is using a Spring {@link WebClient} and caches responses in-memory with default
   * settings
   * @return a (singleton scoped) instance of {@link HalResourceLoader}
   */
  @Bean
  @ConditionalOnMissingBean
  HalResourceLoader halResourceLoader(HalResourceLoaderBuilder halResourceBuilder) {

    return halResourceBuilder
        .withMemoryCache()
        .build();
  }

  /**
   * Provides a {@link HalResourceLoaderBuilder} that is pre-configured with the given {@link HttpClientSupport}
   * @return a {@link HalResourceLoaderBuilder} that can be further customised before a {@link HalResourceLoader} is
   *     built
   */
  @Bean
  @ConditionalOnMissingBean
  HalResourceLoaderBuilder halResourceBuilder(HttpClientSupport httpClientSupport) {
    return HalResourceLoaderBuilder.create()
        .withCustomHttpClient(httpClientSupport);
  }

  /**
   * Provides a {@link WebClientSupport} instance with the given {@link WebClient}
   */
  @Bean
  @ConditionalOnMissingBean
  WebClientSupport webClientSupport(WebClientProvider webClientProvider) {
    return new WebClientSupport(webClientProvider);
  }

  /**
   * Simply provides a static instance of {@link WebClient} for all URIs created with the provided {@link WebClient.Builder}.
   */
  @Bean
  @Scope("prototype")
  @ConditionalOnMissingBean
  WebClientProvider webClientProvider(WebClient.Builder webClientBuilder) {
    WebClient webClient = webClientBuilder.build();
    return uri -> webClient;
  }

  /**
   * Provides a default instance for a {@link WebClient.Builder} with a configured {@link ConnectionProvider}.
   */
  @Bean
  @Scope("prototype")
  @ConditionalOnMissingBean
  WebClient.Builder defaultRhymeWebClientBuilder(ObjectProvider<HttpClientCustomizer> httpClientCustomizerProvider) {
    ConnectionProvider connectionProvider = ConnectionProvider
        .builder(SpringRhymeAutoConfiguration.class.getSimpleName())
        .maxConnections(5000)
        .build();

    HttpClient httpClient = HttpClient.create(connectionProvider);

    httpClientCustomizerProvider.orderedStream().forEach(customizer -> customizer.customize(httpClient));

    return WebClient.builder()
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
        .clientConnector(new ReactorClientHttpConnector(httpClient));
  }

}
