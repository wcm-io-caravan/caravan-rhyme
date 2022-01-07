/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2022 wcm.io
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
package io.wcm.caravan.rhyme.api.client;

import org.osgi.annotation.versioning.ProviderType;

import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.spi.HalApiAnnotationSupport;
import io.wcm.caravan.rhyme.api.spi.HalApiReturnTypeSupport;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.impl.RhymeBuilders;
import io.wcm.caravan.rhyme.impl.renderer.AsyncHalResourceRenderer;

/**
 * A fluent builder to create and customize {@link HalApiClient} instances for advanced integration or test scenarios.
 * <p>
 * If you don't need any of the advanced customizations, you can simply use {@link HalApiClient#create()}
 * {@link HalApiClient#create(HalResourceLoader)} instead.
 * </p>
 * <p>
 * {@link HalApiClientBuilder} instances shouldn't be re-used, as calling the build method may modify its state.
 * </p>
 * @see HalApiClient
 * @see RhymeBuilder
 */
@ProviderType
public interface HalApiClientBuilder {

  /**
   * @return a new {@link HalApiClientBuilder} to start building
   */
  static HalApiClientBuilder create() {
    return RhymeBuilders.client();
  }

  /**
   * @param resourceLoader implements the actual loading (and caching) of JSON/HAL resources
   * @return this
   */
  HalApiClientBuilder withResourceLoader(HalResourceLoader resourceLoader);

  /**
   * A {@link RequestMetricsCollector} instance can be used to track all upstream resources that have been retrieved,
   * and collect metrics and response metadata from the interaction with the proxy objects.
   * This is only relevant for clients created by the {@link Rhyme} instance while handling an incoming
   * request.
   * <p>
   * If you only want to consume HAL APIs and not use Rhyme to render your responses, you don't need
   * to worry about the {@link RequestMetricsCollector}.
   * </p>
   * * @param metricsSharedWithRenderer the same instance that will be used to create the
   * {@link AsyncHalResourceRenderer}
   * @return this
   */
  HalApiClientBuilder withMetrics(RequestMetricsCollector metricsSharedWithRenderer);

  /**
   * Extend the core framework to support additional return types in your annotated HAL API interfaces.
   * You can call this method multiple times if you want to register more than one extension.
   * @param additionalTypeSupport extension to the default type support
   * @return this
   */
  HalApiClientBuilder withReturnTypeSupport(HalApiReturnTypeSupport additionalTypeSupport);

  /**
   * Extend the core framework to support additional annotation types in your annotated HAL API interfaces.
   * You can call this method multiple times if you want to register more than one extension.
   * @param additionalTypeSupport extension to the default type support
   * @return this
   */
  HalApiClientBuilder withAnnotationTypeSupport(HalApiAnnotationSupport additionalTypeSupport);

  /**
   * @return the new {@link HalApiClient} instance
   */
  HalApiClient build();
}
