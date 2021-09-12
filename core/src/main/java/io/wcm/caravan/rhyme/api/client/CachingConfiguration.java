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
package io.wcm.caravan.rhyme.api.client;

import java.util.Optional;

import org.osgi.annotation.versioning.ConsumerType;

import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;

/**
 * A configuration instance for the caching {@link HalResourceLoader} implementations created with
 * {@link HalResourceLoaderBuilder}. It needs to be passed to
 * {@link HalResourceLoaderBuilder#withCachingConfiguration(CachingConfiguration)} to become effective.
 */
@ConsumerType
public interface CachingConfiguration {

  /**
   * Determines for how long a cached response that doesn't have a "cache-control: max-age" directive will be used
   * (before it is considered stale and retrieved again)
   * @param statusCode of the HTTP response (can be empty if the request failed without a status code)
   * @return the number of seconds the response should be served from cache, or 0 if it shouldn't be cached at all
   */
  int getDefaultMaxAge(Optional<Integer> statusCode);

  /**
   * Determines whether the cache should also cache non-successful responses, where a {@link HalApiClientException}
   * was caught and will also throw such an exception on follow-up requests for the same URL
   * @return true if error responses should be cached as well
   */
  boolean isCachingOfHalApiClientExceptionsEnabled();
}
