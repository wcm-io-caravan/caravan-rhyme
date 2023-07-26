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
package io.wcm.caravan.rhyme.impl.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.impl.client.proxy.HalApiClientProxyFactory;
import io.wcm.caravan.rhyme.impl.reflection.HalApiTypeSupport;

/**
 * The implementation of {@link HalApiClient} that delegates the actual loading of resources via the
 * {@link HalResourceLoader} interface to the {@link HalResourceLoaderWrapper}, and creates dynamic
 * client proxies with the {@link HalApiClientProxyFactory}.
 */
public class HalApiClientImpl implements HalApiClient {

  private final HalApiClientProxyFactory factory;

  private final RequestMetricsCollector metrics;
  private final HalApiTypeSupport typeSupport;
  private final RemoteResourceOverrides remoteResourceOverrides;

  /**
   * @param resourceLoader implements the actual loading (and caching) of JSON/HAL resources via any HTTP client library
   * @param metrics an instance of {@link RequestMetricsCollector} to collect performance relevant data for the current
   *          incoming request
   * @param typeSupport the strategy to detect HAL API annotations and perform type conversions
   * @param objectMapper the Jackson {@link ObjectMapper} to use for all JSON deserialization
   * @param overrides provides alternative implementations to be returned by {@link #getRemoteResource(String, Class)}
   */
  public HalApiClientImpl(HalResourceLoader resourceLoader, RequestMetricsCollector metrics, HalApiTypeSupport typeSupport, ObjectMapper objectMapper,
      RemoteResourceOverrides overrides) {

    Preconditions.checkNotNull(resourceLoader, "A " + HalResourceLoader.class.getName() + " instance must be provided");
    HalResourceLoaderWrapper wrapper = new HalResourceLoaderWrapper(resourceLoader, metrics);

    factory = new HalApiClientProxyFactory(wrapper, metrics, typeSupport, objectMapper);

    this.metrics = metrics;
    this.typeSupport = typeSupport;
    this.remoteResourceOverrides = overrides;
  }

  @Override
  public <T> T getRemoteResource(String uri, Class<T> halApiInterface) {

    // first consider any overrides for this interface and URI that may be defined
    return remoteResourceOverrides.get(halApiInterface, uri, metrics)
        // otherwise create a proxy instance that loads the entry point lazily (when required by any method call on the proxy)
        .orElseGet(() -> factory.createProxyFromUrl(halApiInterface, uri));
  }

  public HalApiTypeSupport getTypeSupport() {
    return typeSupport;
  }
}
