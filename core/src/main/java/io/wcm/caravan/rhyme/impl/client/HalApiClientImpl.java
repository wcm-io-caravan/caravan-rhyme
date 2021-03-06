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

import com.google.common.base.Preconditions;

import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.impl.reflection.HalApiTypeSupport;

/**
 * A full implementation of {@link HalApiClientImpl} that delegates the actual loading of resources via the
 * {@link HalResourceLoader} interface
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
  public HalApiClientImpl(HalResourceLoader resourceLoader, RequestMetricsCollector metrics, HalApiTypeSupport typeSupport) {

    Preconditions.checkNotNull(resourceLoader, "A " + HalResourceLoader.class.getName() + " instance must be provided");
    HalResourceLoaderWrapper wrapper = new HalResourceLoaderWrapper(resourceLoader, metrics);

    factory = new HalApiClientProxyFactory(wrapper, metrics, typeSupport);

    this.typeSupport = typeSupport;
  }

  @Override
  public <T> T getRemoteResource(String uri, Class<T> halApiInterface) {

    // create a proxy instance that loads the entry point lazily when required by any method call on the proxy
    return factory.createProxyFromUrl(halApiInterface, uri);
  }

  public HalApiTypeSupport getTypeSupport() {
    return typeSupport;
  }
}
