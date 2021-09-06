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
package io.wcm.caravan.rhyme.api.spi;

import org.osgi.annotation.versioning.ConsumerType;

import io.reactivex.rxjava3.core.Maybe;
import io.wcm.caravan.rhyme.api.client.HalResourceLoaderBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;

/**
 * An SPI interface for a cache implementation that can be given to
 * {@link HalResourceLoaderBuilder#withCustomCache(HalResponseCache)} if you want to store cached HAL+JSON responses
 * in a custom cache (e.g. in an external, persistent data store).
 * <p>
 * Note that it's entirely up to the HalResponseCache
 * implementation to decide for how long a {@link HalResponse} is <b>stored</b> in the cache. Whether the response will
 * but the framework will actually be <b>used</b> is determined by the caching {@link HalResourceLoader}
 * implementation of the core framework, based on the {@link HalResponse#getMaxAge()} value and
 * the time the the response was retrieved (from {@link HalResponse#getTimestamp()}).
 */
@ConsumerType
public interface HalResponseCache {

  /**
   * Tries to retrieve a cached response for the given URI from the cache
   * @param uri the absolute URI of the resource
   * @return a {@link Maybe} that emits a {@link HalResponse} when a cached version is available (and is empty
   *         otherwise)
   */
  Maybe<HalResponse> load(String uri);

  /**
   * Store a HAL response in the cache. It is essential that all fields from the {@link HalResponse} are stored, and
   * retrieved later without modifications.
   * @param uri the absolute URI of the resource
   * @param response the {@link HalResponse} to store
   */
  void store(String uri, HalResponse response);

}
