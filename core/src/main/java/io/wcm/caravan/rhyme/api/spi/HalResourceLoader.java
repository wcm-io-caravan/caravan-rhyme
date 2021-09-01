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
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.impl.client.http.HttpHalResourceLoader;
import io.wcm.caravan.rhyme.impl.client.http.HttpUrlConnectionSupport;

/**
 * An interface to delegate the actual loading and caching of a JSON+HAL resource via HTTP to any other HTTP client
 * library. You can use {@link #withDefaultHttpClient()} to create an instance that is simply using
 * {@link HttpURLConnection} to execute the HTTP requests. If you need additional configuration options (e.g.
 * authentication) or want to use an asynchronous HTTP library then you can call
 * {@link #withCustomHttpClient(HttpClientSupport)} to provide your own implementation of the
 * {@link HttpClientSupport} SPI interface.
 */
@FunctionalInterface
@ConsumerType
public interface HalResourceLoader {

  /**
   * @param uri the URI of the resource to load. The exact format of the URI (i.e. whether it is fully qualified or not)
   *          depends on how the links are represented in the upstream resources
   * @return a {@link Single} that emits a {@link HalResponse} entity if the request was successful, or otherwise fails
   *         with a {@link HalApiClientException}
   */
  Single<HalResponse> getHalResource(String uri);

  static HalResourceLoader withDefaultHttpClient() {

    return HttpHalResourceLoader.withClientImplementation(new HttpUrlConnectionSupport());
  }

  static HalResourceLoader withCustomHttpClient(HttpClientSupport httpClient) {

    return HttpHalResourceLoader.withClientImplementation(httpClient);
  }
}
