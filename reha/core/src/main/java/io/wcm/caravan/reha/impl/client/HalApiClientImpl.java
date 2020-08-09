/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2018 wcm.io
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
package io.wcm.caravan.reha.impl.client;

import com.google.common.base.Preconditions;

import io.wcm.caravan.reha.api.client.HalApiClient;
import io.wcm.caravan.reha.api.common.RequestMetricsCollector;
import io.wcm.caravan.reha.api.spi.JsonResourceLoader;
import io.wcm.caravan.reha.impl.reflection.HalApiTypeSupport;

/**
 * A full implementation of {@link HalApiClientImpl} that delegates the actual loading of resources via the
 * {@link JsonResourceLoader} interface
 */
public class HalApiClientImpl implements HalApiClient {

  private final HalApiClientProxyFactory factory;

  private final HalApiTypeSupport typeSupport;

  /**
   * jsonLoader implements the actual loading (and caching) of JSON/HAL resources via any HTTP client library
   * @param metrics an instance of {@link RequestMetricsCollector} to collect performance relevant data for the current
   *          incoming request
   * @param typeSupport the strategy to detect HAL API annotations and perform type conversions
   */
  public HalApiClientImpl(JsonResourceLoader jsonLoader, RequestMetricsCollector metrics, HalApiTypeSupport typeSupport) {

    Preconditions.checkNotNull(jsonLoader, "A " + JsonResourceLoader.class.getName() + " instance must be provided");
    CachingJsonResourceLoader cachingLoader = new CachingJsonResourceLoader(jsonLoader, metrics);

    factory = new HalApiClientProxyFactory(cachingLoader, metrics, typeSupport);

    this.typeSupport = typeSupport;
  }

  @Override
  public <T> T getEntryPoint(String uri, Class<T> halApiInterface) {

    // create a proxy instance that loads the entry point lazily when required by any method call on the proxy
    return factory.createProxyFromUrl(halApiInterface, uri);
  }

  public HalApiTypeSupport getTypeSupport() {
    return typeSupport;
  }
}
