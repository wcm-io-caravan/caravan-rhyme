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

package io.wcm.caravan.rhyme.api.client;

import org.osgi.annotation.versioning.ProviderType;

import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceRepresentation;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.spi.HalApiAnnotationSupport;
import io.wcm.caravan.rhyme.api.spi.HalApiReturnTypeSupport;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.impl.client.HalApiClientImpl;
import io.wcm.caravan.rhyme.impl.reflection.DefaultHalApiTypeSupport;

/**
 * A type-safe HAL client that will create a dynamic proxy implementation for a given URI
 * and {@link HalApiInterface}.
 * <p>
 * Whenever you call a method on the proxy returned by
 * {@link #getRemoteResource(String, Class)} that is annotated with a relevant annotation (e.g {@link Related}
 * or {@link ResourceState}), an HTTP request to the upstream resource will be triggered,
 * and additional client proxies for the related resources will be created based on the links in the response.
 * This allows you to navigate through the resources simply by calling the methods defined in your HAL API
 * interfaces. The actual loading of the upstream resources is delegated to a {@link HalResourceLoader} instance.
 * </p>
 * <p>
 * The {@link HalApiClient} instance will be caching the proxy objects (based on the URL of the corresponding
 * resources), and the return value of any method called on the proxy objets.
 * This allows you to call the same methods multiple times without the overhead of repeated JSON parsing or
 * duplicate HTTP requests. On the other hand you shouldn't use the same {@link HalApiClient} instance
 * for too long (e.g. for multiple incoming requests to your server), since no invalidation of those caches
 * will take place as long as your are using the same {@link HalApiClient} instance. If you need more persistent
 * caching of the HAL responses, this needs to be implemented as a {@link HalResourceLoader} instead.
 * </p>
 * <p>
 * A {@link RequestMetricsCollector} instance can be used to track all upstream resources that have been retrieved,
 * and collect performance metrics about the interaction with the proxy objects.
 * This is only relevant for clients created by the {@link Rhyme} instance while handling an incoming
 * request. In that case you you shouldn't need to interact with
 * {@link HalApiClient} directly, but use {@link Rhyme#getRemoteResource(String, Class)} instead. This ensures
 * that the same {@link RequestMetricsCollector} instance is used throughout your incoming request.
 * </p>
 * <p>
 * If you only want to consume HAL APIs and not use Rhyme to render your responses, you don't need
 * to worry about the {@link RequestMetricsCollector}, and
 * the easiest way to start is simply calling {@link HalApiClient#create()}.
 * </p>
 * @see HalApiInterface
 * @see Related
 * @see ResourceState
 * @see ResourceRepresentation
 * @see HalResourceLoader
 */
@ProviderType
public interface HalApiClient {

  /**
   * @param uri the absolute URI of the resource to load (usually the entry point of the HAL API)
   * @param halApiInterface the HAL API interface class of a service's entry point resource
   * @return a proxy implementation of the specified entry point interface
   * @param <T> the HAL API interface type
   */
  <T> T getRemoteResource(String uri, Class<T> halApiInterface);

  /**
   * Create a stand-alone {@link HalApiClient} (i.e. not to be used in the lifecycle of a {@link Rhyme} instance).
   * If you need more control over how your HTTP requests are executed, use
   * {@link #create(HalResourceLoader, RequestMetricsCollector)} instead.
   * @return an instance of {@link HalApiClient} that uses a default HTTP client implementation
   */
  static HalApiClient create() {

    return HalApiClient.create(HalResourceLoader.withDefaultHttpClient(), RequestMetricsCollector.create());
  }

  /**
   * @param resourceLoader implements the actual loading (and caching) of JSON/HAL resources via any HTTP client library
   * @param metrics an instance of {@link RequestMetricsCollector} to collect performance relevant data for the current
   *          incoming request
   * @return an instance of {@link HalApiClient} that should be re-used for all upstream requests required by the
   *         current incoming request
   */
  static HalApiClient create(HalResourceLoader resourceLoader, RequestMetricsCollector metrics) {

    return new HalApiClientImpl(resourceLoader, metrics, new DefaultHalApiTypeSupport());
  }

  /**
   * An advanced overload {@link #create(HalResourceLoader, RequestMetricsCollector)} that allows
   * to the client to be used with interfaces using non-standard annotations or return types.
   * @param resourceLoader implements the actual loading (and caching) of JSON/HAL resources via any HTTP client library
   * @param metrics an instance of {@link RequestMetricsCollector} to collect performance relevant data for the current
   *          incoming request
   * @param annotationSupport an (optional) strategy to identify HAL API interfaces and methods that use different
   *          annotations
   * @param returnTypeSupport an (optional) strategy to support additional return types in your HAL API interface
   *          methods
   * @return an instance of {@link HalApiClient} that should be re-used for all upstream requests required by the
   *         current incoming request
   */
  static HalApiClient create(HalResourceLoader resourceLoader, RequestMetricsCollector metrics,
      HalApiAnnotationSupport annotationSupport, HalApiReturnTypeSupport returnTypeSupport) {

    return new HalApiClientImpl(resourceLoader, metrics, DefaultHalApiTypeSupport.extendWith(annotationSupport, returnTypeSupport));
  }
}
