/*-
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2014 - 2020 wcm.io Caravan
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

package io.wcm.caravan.reha.api.client;

import org.osgi.annotation.versioning.ProviderType;

import io.wcm.caravan.reha.api.annotations.RelatedResource;
import io.wcm.caravan.reha.api.annotations.ResourceState;
import io.wcm.caravan.reha.api.common.RequestMetricsCollector;
import io.wcm.caravan.reha.api.spi.HalApiAnnotationSupport;
import io.wcm.caravan.reha.api.spi.HalApiReturnTypeSupport;
import io.wcm.caravan.reha.api.spi.JsonResourceLoader;
import io.wcm.caravan.reha.impl.client.HalApiClientImpl;
import io.wcm.caravan.reha.impl.reflection.DefaultHalApiTypeSupport;

/**
 * A generic HAL API client that will provide a dynamic proxy implementation of a given HAL API interface
 * which loads data via HTTP as required when the interface methods annotated with {@link RelatedResource},
 * {@link ResourceState} etc are called.
 */
@ProviderType
public interface HalApiClient {

  /**
   * @param uri the absolute path (and query parameters) of the entry point
   * @param halApiInterface the HAL API interface class of a service's entry point resource
   * @return a proxy implementation of the specified entry point interface
   * @param <T> the HAL API interface type
   */
  <T> T getEntryPoint(String uri, Class<T> halApiInterface);

  /**
   * @param jsonLoader implements the actual loading (and caching) of JSON/HAL resources via any HTTP client library
   * @param metrics an instance of {@link RequestMetricsCollector} to collect performance relevant data for the current
   *          incoming request
   * @return an instance of {@link HalApiClient} that should be re-used for all upstream requests required by the
   *         current incoming request
   */
  static HalApiClient create(JsonResourceLoader jsonLoader, RequestMetricsCollector metrics) {

    return new HalApiClientImpl(jsonLoader, metrics, new DefaultHalApiTypeSupport());
  }

  /**
   * @param jsonLoader implements the actual loading (and caching) of JSON/HAL resources via any HTTP client library
   * @param metrics an instance of {@link RequestMetricsCollector} to collect performance relevant data for the current
   *          incoming request
   * @param annotationSupport an (optional) strategy to identify HAL API interfaces and methods that use different
   *          annotations
   * @param returnTypeSupport an (optional) strategy to support additional return types in your HAL API interface
   *          methods
   * @return an instance of {@link HalApiClient} that should be re-used for all upstream requests required by the
   *         current incoming request
   */
  static HalApiClient create(JsonResourceLoader jsonLoader, RequestMetricsCollector metrics,
      HalApiAnnotationSupport annotationSupport, HalApiReturnTypeSupport returnTypeSupport) {

    return new HalApiClientImpl(jsonLoader, metrics, DefaultHalApiTypeSupport.extendWith(annotationSupport, returnTypeSupport));
  }
}
