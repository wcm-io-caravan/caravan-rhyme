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

package io.wcm.caravan.rhyme.api.spi;

import java.net.HttpURLConnection;

import org.osgi.annotation.versioning.ConsumerType;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.client.HalResourceLoaderBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.impl.client.HalResourceLoaderBuilderImpl;

/**
 * An interface to delegate (or mock) the actual loading (and caching) of all JSON+HAL resources
 * loaded via {@link HalApiClient} and {@link Rhyme} to any other HTTP client
 * client implementation. The implementation needs to be thread safe, and the same
 * instance can be re-used throughout your application life-cycle.
 * <p>
 * If you don't need any configuration options (e.g. authentication), caching or asynchronous request handling,
 * you can can simply use {@link #withDefaultHttpClient()} to create an instance that is using
 * {@link HttpURLConnection} to execute the HTTP requests while blocking the current request.
 * </p>
 * <p>
 * If you need full control over the HTTP client implementation to be used, and/or want to enable
 * caching of HAL responses, you should call {@link #builder()} to configure and build
 * a {@link HalResourceLoader} instance with the methods from {@link HalResourceLoaderBuilder}.
 * For caching to work properly, it is important that you'll then store and re-use the same
 * {@link HalResourceLoader} instance when creating {@link Rhyme} or {@link HalApiClient} objects
 * </p>
 * <p>
 * You can also implement this interface yourself, but this will require you to
 * also implement the JSON parsing and exception handling according to the expectations of the
 * {@link HalApiClient} implementation. A simpler way is to implement the callback-style
 * {@link HttpClientSupport} interface, and then use
 * {@link HalResourceLoaderBuilder#withCustomHttpClient(HttpClientSupport)}
 * to adapt it. In both cases, you should extend
 * the io.wcm.caravan.rhyme.testing.client.AbstractHalResourceLoaderTest (from the testing module)
 * to test your implementation against a Wiremock server, to ensure that all expectations
 * regarding response and error handling are met.
 * </p>
 * @see HttpClientSupport
 * @see HalResourceLoaderBuilder
 * @see RhymeBuilder#withResourceLoader(HalResourceLoader)
 * @see HalApiClient#create(HalResourceLoader)
 */
@FunctionalInterface
@ConsumerType
public interface HalResourceLoader {

  /**
   * Provides the response to a GET request for the given URL. The returned {@link Single} will only
   * emit a {@link HalResponse} if the request was successful (i.e. a 200 status with a valid JSON body was returned).
   * If anything goes wrong during the request cycle, the {@link Single} fails with {@link HalApiClientException}
   * which gives you access to the status code and root cause of the failure.
   * @param uri the URI of the resource to load. This is usually a fully qualified HTTP(S) URL,
   *          but it could be any URI (depending on how the entry point was loaded and which kind of links are
   *          represented in the upstream resources)
   * @return a {@link Single} that emits a {@link HalResponse} entity if the request was successful, or otherwise fails
   *         with a {@link HalApiClientException}
   */
  Single<HalResponse> getHalResource(String uri);


  static HalResourceLoaderBuilder builder() {

    return new HalResourceLoaderBuilderImpl();
  }

  /**
   * Create a {@link HalResourceLoader} that uses a {@link HttpURLConnection} with default configuration to
   * load the upstream resources.
   * @return a {@link HalResourceLoader} that can handle fully qualified HTTP or HTTPS URIs,
   *         blocks the current thread while loading the resource,
   *         and does not implement any caching
   */
  static HalResourceLoader withDefaultHttpClient() {

    // if no further methods are called, the builder will create a loader that uses HttpURLConnection (and no caching)
    return builder().build();
  }

}
