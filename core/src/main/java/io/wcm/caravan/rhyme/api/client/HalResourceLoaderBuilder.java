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

import java.net.HttpURLConnection;
import java.time.Clock;
import java.time.Duration;

import org.osgi.annotation.versioning.ProviderType;

import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.api.spi.HalResponseCache;
import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;
import io.wcm.caravan.rhyme.impl.client.HalResourceLoaderBuilderImpl;

/**
 * A builder to create and configure a {@link HalResourceLoader} instance that is using a custom HTTP client
 * implementation and/or uses a {@link HalResponseCache} to cache HTTP responses.
 * <p>
 * By default (i.e. without calling any methods before {@link #build()}) this builder will create a
 * {@link HalResourceLoader}
 * that is not caching anything, and uses {@link HttpURLConnection} to retrieve responses synchronously. You can use
 * {@link #withCustomHttpClient(HttpClientSupport)} to use a different {@link HttpClientSupport} implementation for all
 * HTTP requests.
 * </p>
 * <p>
 * To enable caching of HTTP responses, you must call {@link #withMemoryCache()},
 * {@link #withMemoryCache(int, Duration)} or {@link #withCustomCache(HalResponseCache)}, and then ensure that the same
 * {@link HalResourceLoader} instance is re-used throughout the life-time of your application.
 * </p>
 * <p>
 * The caching logic will use the {@link HalResponse#getMaxAge()} value to determine for how long a response
 * from cache can be used before it is considered stale. If a stale value for a specific URL is found in cache, it is
 * ignored and another upstream request for this URL will be triggered.
 * </p>
 * <p>
 * By default, only 200/OK responses are being cached, and if the upstream responses do not contain any max-age header,
 * they are only cached for max of 60 seconds. This behaviour can be changed by providing an alternative configuration
 * via {@link #withCachingConfiguration(CachingConfiguration)}.
 * </p>
 * @see RhymeBuilder#withResourceLoader(HalResourceLoader)
 * @see HalApiClient#create(HalResourceLoader)
 * @see HttpClientSupport
 * @see HalResponseCache
 * @see CachingConfiguration
 */
@ProviderType
public interface HalResourceLoaderBuilder {

  /**
   * @return a new instance of {@link HalResourceLoaderBuilder}
   */
  public static HalResourceLoaderBuilder create() {
    return new HalResourceLoaderBuilderImpl();
  }

  /**
   * Replace the default HTTP client with the given implementation.
   * @param client the {@link HttpClientSupport} implementation to use
   * @return a new instance of {@link HalResourceLoaderBuilder}
   */
  HalResourceLoaderBuilder withCustomHttpClient(HttpClientSupport client);

  /**
   * Enable in-memory caching of responses with default settings. The in-memory cache can contain up to 10.000 HAL+JSON
   * responses, and responses that haven't been read from cache for more than one day will be automatically discarded.
   * If your HAL responses have an average size of 100kb, this can take up to 1GB of memory. You can use
   * {@link #withMemoryCache(int, Duration)} if you want to use a cache with a smaller or larger memory footprint.
   * @return a new instance of {@link HalResourceLoaderBuilder}
   */
  HalResourceLoaderBuilder withMemoryCache();

  /**
   * Enable in-memory caching of responses with custom settings
   * @param maxNumItems the total number of HAL responses that can be stored in cache
   * @param timeToIdle the duration after which a response that hasn't been read from cache any more will be discarded
   * @return a new instance of {@link HalResourceLoaderBuilder}
   */
  HalResourceLoaderBuilder withMemoryCache(int maxNumItems, Duration timeToIdle);

  /**
   * Enable caching of responses using a custom caching implementation. This can be used to cache responses in an
   * external, persistent data store.
   * @param cacheImplementation an implementation of {@link HalResponseCache}
   * @return a new instance of {@link HalResourceLoaderBuilder}
   */
  HalResourceLoaderBuilder withCustomCache(HalResponseCache cacheImplementation);

  /**
   * Replace the default caching configuration that determines which responses are being cached and for how long
   * responses without a cache-control/max-age header will be used.
   * If you don't call this method, only 200/OK responses will be cached, and a max-age
   * of 60 seconds will be used for responses without a max-age value in the header.
   * @param config the desired configuration to be used
   * @return a new instance of {@link HalResourceLoaderBuilder}
   */
  HalResourceLoaderBuilder withCachingConfiguration(CachingConfiguration config);

  /**
   * Use a custom {@link HalResourceLoader} implementation instead of the default HTTP client. This method only is
   * required if you do already have a full implementation of {@link HalResourceLoader}, for which you want to
   * add caching logic by calling one of the caching-related methods of this builder
   * @param resourceLoader an existing {@link HalResourceLoader} instance
   * @return a new instance of {@link HalResourceLoaderBuilder}
   */
  HalResourceLoaderBuilder withExistingLoader(HalResourceLoader resourceLoader);

  /**
   * Replace the clock that the caching logic uses to determine the age of cached responses. Only useful for unit-tests
   * where you want to simulate time advancing.
   * @param customClock a mutable clock to be use instead of the system clock
   * @return a new instance of {@link HalResourceLoaderBuilder}
   */
  HalResourceLoaderBuilder withClock(Clock customClock);

  /**
   * Create the {@link HalResourceLoader} with the HTTP and caching configuration defined by the previous methods calls
   * @return a {@link HalResourceLoader} instance that you should re-use for multiple calls to
   *         {@link RhymeBuilder#withResourceLoader(HalResourceLoader)} or
   *         {@link HalApiClient#create(HalResourceLoader)}
   */
  HalResourceLoader build();

}
