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

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import io.wcm.caravan.rhyme.api.client.HalResourceLoaderBuilder;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.spring.api.SpringRhyme;

/**
 * Defines default implementations for spring beans required by {@link SpringRhyme}. You can override
 * any of these bean definitions if you need a different implementation in your project (or tests)
 */
@Configuration
@ComponentScan(basePackages = "io.wcm.caravan.rhyme.spring.impl")
class SpringRhymeAutoConfiguration {

  /**
   * Defines the default resource loader used by the {@link SpringRhyme} implementation class
   * @param builder used to create the underlying {@link WebClient}
   * @return a (singleton scoped) instance of {@link HalResourceLoader}
   */
  @Bean
  @ConditionalOnMissingBean
  HalResourceLoader halResourceLoader(WebClient.Builder builder) {

    return halResourceBuilder(builder)
        .withMemoryCache()
        .build();
  }

  /**
   * Provides a {@link HalResourceLoaderBuilder} that is already pre-configured to use a {@link WebClientSupport}
   * @param builder used to create the underlying {@link WebClient}
   * @return a {@link HalResourceLoaderBuilder} that can be further customised before a {@link HalResourceLoader} is
   *         built
   */
  @Bean
  @ConditionalOnMissingBean
  HalResourceLoaderBuilder halResourceBuilder(WebClient.Builder builder) {

    WebClientSupport client = new WebClientSupport(builder::build);

    return HalResourceLoader.builder()
        .withCustomHttpClient(client);
  }
}
